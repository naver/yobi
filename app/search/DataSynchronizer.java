package search;

import akka.actor.Cancellable;
import models.*;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import play.Configuration;
import play.Logger;
import play.libs.Akka;
import scala.concurrent.duration.Duration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.query.QueryBuilders.idsQuery;
import static org.elasticsearch.index.query.QueryBuilders.indicesQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

/**
 * @author Keeun Baik
 */
public class DataSynchronizer {

    private TransportClient client;
    private SearchEngine searchEngine;
    private Cancellable synchronizingSchedule;
    private static final long ONE_MINUTES = 60 * 1000l;

    private boolean synchronizing = false;

    public DataSynchronizer(SearchEngine searchEngine, TransportClient client) {
        this.searchEngine = searchEngine;
        this.client = client;
    }

    public void start() {
        this.synchronizing = true;
    }

    public void end() {
        this.synchronizing = false;
    }

    public boolean isAvailable() {
        return searchEngine.isAvailable() && !this.synchronizing;
    }


    /**
     * Synchronize data that are supposed to be index, update or delete into ElasticSearch
     */
    public void startSynchronization() {
        Runnable polling = new Runnable() {
            @Override
            public void run() {
                if (searchEngine.isAvailable()) {
                    start();
                    for (PendingData pendingData : PendingData.find.findList()) {
                        synchronize(pendingData);
                    }
                    end();
                }
            }
        };

        synchronizingSchedule = Akka.system().scheduler().schedule(
                Duration.create(0, TimeUnit.MINUTES),
                Duration.create(
                    Configuration.root().getMilliseconds("application.search.data.synchronization.interval", ONE_MINUTES),
                    TimeUnit.MILLISECONDS),
                polling,
                Akka.system().dispatcher()
        );
    }

    private void synchronize(PendingData pendingData) {
        switch (pendingData.dataType) {
            case PROJECT:
                synchronize(pendingData, Project.find.byId(pendingData.dataId));
                break;
            case USER:
                synchronize(pendingData, User.find.byId(pendingData.dataId));
                break;
            case ISSUE:
                synchronize(pendingData, Issue.finder.byId(pendingData.dataId));
                break;
        }
    }

    private void synchronize(PendingData pendingData, Project project) {
        switch (pendingData.dataOperation) {
            case INDEX:
                index(pendingData, project);
                break;
            case UPDATE:
                update(pendingData, project);
                break;
            case DELETE:
                // At this point, the project can be null, because it can be already deleted from database.
                // so, make a fake object that contains only project's id.
                Project fakeProject = new Project();
                fakeProject.id = pendingData.dataId;
                delete(pendingData, fakeProject);
                break;
        }
    }

    private void synchronize(PendingData pendingData, User user) {
        switch (pendingData.dataOperation) {
            case INDEX:
                index(pendingData, user);
                break;
            case UPDATE:
                update(pendingData, user);
                break;
            case DELETE:
                delete(pendingData, user);
                break;
        }
    }

    private void synchronize(PendingData pendingData, Issue issue) {
        switch (pendingData.dataOperation) {
            case INDEX:
                index(pendingData, issue);
                break;
            case UPDATE:
                update(pendingData, issue);
                break;
            case DELETE:
                delete(pendingData, issue);
                break;
        }
    }

    public void stopSynchronization() {
        if (synchronizingSchedule != null && !synchronizingSchedule.isCancelled()) {
            synchronizingSchedule.cancel();
        }
    }

    /**
     * Index new {@code project}
     *
     * @param project
     */
    public void index(@Nullable final PendingData pendingData, @Nonnull final Project project) {
        index(pendingData, project, SearchEngine.TYPE_PROJECT);
    }

    /**
     * Update the document of the {@code project}
     *
     * @param pendingData
     * @param project
     */
    public void update(@Nullable final PendingData pendingData, @Nonnull final Project project) {
        update(pendingData, project, SearchEngine.TYPE_PROJECT);
    }

    /**
     * If {@code pendingData} is null, it means the ElasticSearch is available now.
     * So, synchronize all data related with deleting the {@code project}:
     * - Delete the document of the {@code project}.
     * - Delete all documents(IssueComment, Issue, PostingComment, Posting, Milestone, ReviewComment)
     * related with the {@code project}.
     * - Update user documents to synchronize project's member data.
     *
     * If {@code pendingData} is not null, it means this is called with fake project (has only id property) and
     * recorded other all related synchronization works into PendingData table.
     * So, just delete document related with the {@code project}.
     *
     * @param pendingData
     * @param project
     */
    public void delete(@Nullable final PendingData pendingData, @Nonnull final Project project) {
        if (pendingData == null) {
            BulkRequestBuilder bulkRequest = client.prepareBulk();

            bulkRequest.add(client.prepareDelete().setIndex(SearchEngine.INDEX_NAME).setType(SearchEngine.TYPE_PROJECT).setId(project.indexId()));

            for (PostingComment postingComment : PostingComment.find.where().eq("posting.project", project).findList()) {
                bulkRequest.add(client.prepareDelete().setIndex(SearchEngine.INDEX_NAME).setType(SearchEngine.TYPE_POSTING_COMMENT).setId(postingComment.indexId()));
            }

            for (Posting posting: Posting.finder.where().eq("project", project).findList()) {
                bulkRequest.add(client.prepareDelete().setIndex(SearchEngine.INDEX_NAME).setType(SearchEngine.TYPE_POST).setId(posting.indexId()));
            }

            for (IssueComment issueComment : IssueComment.find.where().eq("issue.project", project).findList()) {
                bulkRequest.add(client.prepareDelete().setIndex(SearchEngine.INDEX_NAME).setType(SearchEngine.TYPE_ISSUE_COMMENT).setId(issueComment.indexId()));
            }

            for (Issue issue : Issue.finder.where().eq("project", project).findList()) {
                bulkRequest.add(client.prepareDelete().setIndex(SearchEngine.INDEX_NAME).setType(SearchEngine.TYPE_ISSUE).setId(issue.indexId()));
            }

            for (Milestone milestone: Milestone.find.where().eq("project", project).findList()) {
                bulkRequest.add(client.prepareDelete().setIndex(SearchEngine.INDEX_NAME).setType(SearchEngine.TYPE_MILESTONE).setId(milestone.indexId()));
            }

            for (ReviewComment reviewComment: ReviewComment.find.where().eq("thread.project", project).findList()) {
                bulkRequest.add(client.prepareDelete().setIndex(SearchEngine.INDEX_NAME).setType(SearchEngine.TYPE_REVIEW).setId(reviewComment.indexId()));
            }

            for (User user : User.findUsersByProject(project.id)) {
                bulkRequest.add(client.prepareUpdate().setIndex(SearchEngine.INDEX_NAME).setType(SearchEngine.TYPE_USER).setDoc(user.source()));
            }

            bulkRequest.execute(new ActionListener<BulkResponse>() {
                @Override
                public void onResponse(BulkResponse bulkItemResponses) {
                    Logger.debug("[SearchEngine] deleted {{}} type {{}}", SearchEngine.TYPE_PROJECT, project.indexId());
                }

                @Override
                public void onFailure(Throwable e) {
                    Logger.debug("[SearchEngine] failed to delete {{}} type {{}}", SearchEngine.TYPE_PROJECT, project.indexId());
                }
            });
        } else {
            client.prepareDelete().setIndex(SearchEngine.INDEX_NAME).setType(SearchEngine.TYPE_PROJECT).setId(project.indexId())
                .execute(new ActionListener<DeleteResponse>() {
                    @Override
                    public void onResponse(DeleteResponse deleteResponse) {
                        Logger.debug("[SearchEngine] deleted {{}} type {{}}", SearchEngine.TYPE_PROJECT, project.indexId());
                        pendingData.delete();
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        Logger.debug("[SearchEngine] failed to delete {{}} type {{}}", SearchEngine.TYPE_PROJECT, project.indexId());
                    }
                });
        }
    }

    /**
     * Index new {@code user}
     *
     * @param user
     */
    public void index(@Nullable PendingData pendingData, @Nonnull User user) {
        index(pendingData, user, SearchEngine.TYPE_USER);
    }

    /**
     * Update the document of the {@code user}
     *
     * @param pendingData
     * @param user
     */
    public void update(@Nullable PendingData pendingData, @Nonnull User user) {
        update(pendingData, user, SearchEngine.TYPE_USER);
    }

    /**
     * Delete the document of the {@code user}.
     *
     * @param pendingData
     * @param user
     */
    public void delete(final PendingData pendingData, final User user) {
        client.prepareDelete().setIndex(SearchEngine.INDEX_NAME).setType(SearchEngine.TYPE_USER).setId(user.indexId())
            .execute(new ActionListener<DeleteResponse>() {
                @Override
                public void onResponse(DeleteResponse deleteResponse) {
                    Logger.debug("[SearchEngine] deleted {{}} type {{}}", SearchEngine.TYPE_USER, user.indexId());
                    pendingData.delete();
                }

                @Override
                public void onFailure(Throwable e) {
                    Logger.debug("[SearchEngine] failed to delete {{}} type {{}}", SearchEngine.TYPE_USER, user.indexId());
                }
            });
    }

    public void index(PendingData pendingData, Issue issue) {

    }

    public void update(PendingData pendingData, Issue issue) {

    }

    public void delete(PendingData pendingData, Issue issue) {

    }

    public void index(PendingData pendingData, Posting posting) {

    }

    public void update(PendingData pendingData, Posting posting) {

    }

    public void delete(PendingData pendingData, Posting posting) {

    }

    public void index(PendingData pendingData, IssueComment issueComment) {

    }

    public void update(PendingData pendingData, IssueComment issueComment) {

    }

    public void delete(PendingData pendingData, IssueComment issueComment) {

    }

    public void index(PendingData pendingData, PostingComment postingComment) {

    }

    public void update(PendingData pendingData, PostingComment postingComment) {

    }

    public void delete(PendingData pendingData, PostingComment postingComment) {

    }

    public void index(PendingData pendingData, Milestone milestone) {

    }

    public void update(PendingData pendingData, Milestone milestone) {

    }

    public void delete(PendingData pendingData, Milestone milestone) {

    }

    public void index(PendingData pendingData, ReviewComment reviewComment) {

    }

    public void update(PendingData pendingData, ReviewComment reviewComment) {

    }

    public void delete(PendingData pendingData, ReviewComment reviewComment) {

    }

    public void index(Project project) {
        index(null, project);
    }

    public void update(Project project) {
        update(null, project);
    }

    public void delete(Project project) {
        delete(null, project);
    }

    public void index(User user) {
        index(null, user);
    }

    public void update(User user) {
        update(null, user);
    }

    public void delete(User user) {
        delete(null, user);
    }

    /**
     * Index new document for the {@code indexable} into the {@code type}
     * If the {@code pendingData} is not null, delete it after get response.
     *
     * @param pendingData
     * @param indexable
     * @param type
     */
    private void index(@Nullable final PendingData pendingData, @Nonnull final Indexable indexable, @Nonnull final String type) {
        client.prepareIndex(SearchEngine.INDEX_NAME, type, indexable.indexId())
                .setSource(indexable.source()).execute(new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {
                Logger.debug("[SearchEngine] indexed {{}} type {{}}", type, indexable.indexId());
                if (pendingData != null) {
                    pendingData.delete();
                }
            }

            @Override
            public void onFailure(Throwable e) {
                Logger.debug("[SearchEngine] failed to index {{}} type {{}}", type, indexable.indexId());
            }
        });
    }

    /**
     * Update document for the {@code indexable} into the {@code type}
     * If the {@code pendingData} is not null, delete it after get response.
     *
     * @param pendingData
     * @param indexable
     * @param type
     */
    private void update(@Nullable final PendingData pendingData, @Nonnull final Indexable indexable, @Nonnull final String type) {
        client.prepareUpdate(SearchEngine.INDEX_NAME, type, indexable.indexId())
                .setDoc(indexable.source()).execute(new ActionListener<UpdateResponse>() {
            @Override
            public void onResponse(UpdateResponse updateResponse) {
                Logger.debug("[SearchEngine] updated {{}} type {{}}", type, indexable.indexId());
                if (pendingData != null) {
                    pendingData.delete();
                }
            }

            @Override
            public void onFailure(Throwable e) {
                Logger.debug("[SearchEngine] failed to update {{}} type {{}}", type, indexable.indexId());
            }
        });
    }
}
