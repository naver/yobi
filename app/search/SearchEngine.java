package search;

import com.avaje.ebean.*;
import models.*;
import models.enumeration.Operation;
import models.enumeration.SearchType;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import utils.AccessControl;
import utils.Diagnostic;
import utils.SimpleDiagnostic;

import javax.annotation.Nonnull;
import java.util.*;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * @author Keeun Baik
 */
public class SearchEngine {

    public static final String INDEX_NAME = "yobi";

    public static final String TYPE_PROJECT = "project";
    public static final String TYPE_ISSUE = "issue";
    public static final String TYPE_USER = "user";
    public static final String TYPE_ISSUE_COMMENT = "issue_comment";
    public static final String TYPE_POST = "post";
    public static final String TYPE_MILESTONE = "milestone";
    public static final String TYPE_POSTING_COMMENT = "post_comment";
    public static final String TYPE_REVIEW = "review";

    private static final int DEFAULT_BULK_SIZE = 1000;

    private TransportClient client;
    private DataFinder finder;
    private DataSynchronizer synchronizer;

    private static boolean USE_ELASTIC = true;

    /**
     * If this option is true, delete index and reset all related data.
     * It will take long time for indexing all data.
     **
     * You can use this option when you've changed the type to index,
     * so that you want to re-index all data again.
     */
    private static boolean REINDEX = false;

    private static boolean REINDEX_PROJECTS = false;
    private static boolean REINDEX_USERS = false;
    private static boolean REINDEX_ISSUES = false;
    private static boolean REINDEX_ISSUE_COMMENTS = false;
    private static boolean REINDEX_POSTS = false;
    private static boolean REINDEX_MILESTONE = false;
    private static boolean REINDEX_POSTING_COMMENT = false;
    private static boolean REINDEX_REVIEW = false;

    private static final SearchEngine SINGLETON = new SearchEngine();

    public static SearchEngine getObject() {
        return SINGLETON;
    }

    private SearchEngine() {
        Settings settings = ImmutableSettings.settingsBuilder()
            .put("cluster.name", "yobi-elasticsearch")
            .put("client.transport.sniff", true)
            .build();

        client = new TransportClient(settings);
        client.addTransportAddress(new InetSocketTransportAddress("localhost", 9300)); // TODO 설정으로 빼내기

        finder = new DataFinder(client);
        synchronizer = new DataSynchronizer(this, client);
    }

    /**
     * Build a node's client
     *
     * @see <a href="http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/current/client.html">client</a>
     */
    public void start() {
        if (!USE_ELASTIC) {
            play.Logger.info("[SearchEngine] is not enabled.");
            return;
        }

        play.Logger.debug("[SearchEngine] started");

        if (!isAvailable()) {
            play.Logger.info("[SearchEngine] stopped, cause the ElasticSearch is not available");
            return;
        }

        if (REINDEX) {
            deleteIndex();
        }

        if (createIndex()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        synchronizer.start();
                        index(REINDEX_PROJECTS, TYPE_PROJECT, Project.find.fetch("labels").setReadOnly(true));
                        index(REINDEX_USERS, TYPE_USER, User.find.query().setReadOnly(true));
                        index(REINDEX_ISSUES, TYPE_ISSUE, Issue.finder.fetch("project").fetch("labels").fetch("assignee").setReadOnly(true));
                        index(REINDEX_ISSUE_COMMENTS, TYPE_ISSUE_COMMENT, IssueComment.find.fetch("issue").setReadOnly(true));
                        index(REINDEX_POSTS, TYPE_POST, Posting.finder.setReadOnly(true));
                        index(REINDEX_MILESTONE, TYPE_MILESTONE, Milestone.find.setReadOnly(true));
                        index(REINDEX_POSTING_COMMENT, TYPE_POSTING_COMMENT, PostingComment.find.fetch("posting").setReadOnly(true));
                        index(REINDEX_REVIEW, TYPE_REVIEW, ReviewComment.find.fetch("thread").setReadOnly(true));
                        synchronizer.end();
                    } catch (Exception e) {
                        synchronizer.end();
                    }
                }
            }).start();

            synchronizer.startSynchronization();
        }
    }

    public void stop() {
        client.close();
        synchronizer.stopSynchronization();
        play.Logger.debug("[SearchEngine] closed");
    }

    private void deleteIndex() {
        DeleteIndexResponse deleteIndexResponse = client.admin().indices().prepareDelete(INDEX_NAME).execute().actionGet();
        if (deleteIndexResponse.isAcknowledged()) {
            play.Logger.debug("[SearchEngine] delete index");
        }
    }

    public boolean isAvailable() {
        ClusterStatsResponse response = null;
        try {
            response = client.admin().cluster().prepareClusterStats().execute().actionGet();
        } catch (ElasticsearchException e) {
            play.Logger.info("[SearchEngine] is not available");
            return false;
        }

        if (response != null && response.getStatus() != ClusterHealthStatus.RED) {
            return true;
        } else {
            return false;
        }
    }

    private boolean createIndex() {
        boolean exist = false;
        GetIndexResponse getIndexResponse = client.admin().indices().prepareGetIndex().execute().actionGet();
        for (String index : getIndexResponse.indices()) {
            if (index.equals(INDEX_NAME)) {
                play.Logger.debug("[SearchEngine] found index {{}}", INDEX_NAME);
                exist = true;
                break;
            }
        }

        if (exist) {
            return true;
        } else {
            try {
                CreateIndexResponse createIndexResponse = client.admin().indices().prepareCreate(INDEX_NAME).execute().actionGet();

                if (createIndexResponse.isAcknowledged()) {
                    play.Logger.debug("[SearchEngine] created index {{}}", INDEX_NAME);
                }

                return true;
            } catch (Exception e) {
                if (e instanceof IndexAlreadyExistsException) {
                    play.Logger.debug("[SearchEngine] {{}} already exists", INDEX_NAME);
                    return true;
                } else {
                    play.Logger.debug("[SearchEngine] failed to create index {{}}", e.getMessage());
                    return false;
                }
            }
        }
    }

    private void registerDiagnostic() {
        Diagnostic.register(new SimpleDiagnostic() {
            @Override
            public String checkOne() {
                if (!isAvailable()) {
                    return "The ElasticSearch is not available";
                } else {
                    return null;
                }
            }
        });
    }

    private long countDocuments(String type) {
        CountResponse countResponse = client.prepareCount(INDEX_NAME)
                .setQuery(termQuery("_type", type))
                .execute()
                .actionGet();

        long count = countResponse.getCount();
        play.Logger.debug("[SearchEngine] Index has {{}} {{}}", count, type);
        return count;
    }

    private void refreshIndex() {
        client.admin().indices().prepareRefresh(INDEX_NAME).execute().actionGet();
    }

    private <T extends Indexable> void index(boolean reindex, String type, Query<T> query) {
        if (reindex) {
            deleteDocuments(type);
        }

        if (isEmpty(type)) {
            int pageSize = DEFAULT_BULK_SIZE;
            int totalPageCount = query.findPagingList(pageSize).getTotalPageCount();
            play.Logger.debug("[SearchEngine] {{}} has {{}} pages to index.", type, totalPageCount);
            for (int page = 0 ; page < totalPageCount ; page++) {
                play.Logger.debug("[SearchEngine] {{}} page {{}} started to index.", type, page);
                BulkRequestBuilder bulkRequest = client.prepareBulk();
                int bulkSize = 0;
                for (T entity : query.findPagingList(pageSize).getPage(page).getList()) {
                    try {
                        bulkRequest.add(client.prepareIndex().setIndex(INDEX_NAME)
                                .setType(type)
                                .setId(entity.indexId())
                                .setSource(entity.source()));
                        bulkSize++;
                    } catch (Exception e) {
                        play.Logger.error("[SearchEngine] {{}}#{{}} failed to add to bulk request, cause {{}}", entity, entity.indexId(), e.getMessage());
                    }
                }
                BulkResponse bulkResponse = bulkRequest.execute().actionGet();
                if (bulkResponse.hasFailures()) {
                    play.Logger.warn("[SearchEngine] failed to execute {{}}", bulkResponse.buildFailureMessage());
                } else {
                    play.Logger.debug("[SearchEngine] {{}} page {{}} size {{}} indexed.", type, page, bulkSize);
                }
            }

            refreshIndex();
            countDocuments(type);
        }
    }

    private boolean isEmpty(String type) {
        return countDocuments(type) == 0l;
    }

    private void deleteDocuments(@Nonnull String type) {
        client.prepareDeleteByQuery(INDEX_NAME)
                .setQuery(termQuery("_type", type))
                .execute()
                .actionGet();
        play.Logger.debug("[SearchEngine] deleted type {{}}", type);
    }

    public @Nonnull SearchResult searchInAll(String keyword, User user, SearchType searchType, PageParam pageParam) {
        List<Project> projects = finder.getReadableProjects(user);

        SearchResult searchResult = new SearchResult();
        searchResult.setKeyword(keyword);
        searchResult.setSearchType(searchType);
        searchResult.setUsersCount(finder.countUsers(keyword));
        searchResult.setIssuesCount(finder.countIssues(keyword, user, projects));
        searchResult.setProjectsCount(finder.countProjects(keyword, projects));
        searchResult.setPostsCount(finder.countPosts(keyword, user, projects));
        searchResult.setMilestonesCount(finder.countMilestones(keyword, projects));
        searchResult.setIssueCommentsCount(finder.countIssueComments(keyword, user, projects));
        searchResult.setPostCommentsCount(finder.countPostComments(keyword, user, projects));
        searchResult.setReviewsCount(finder.countReviews(keyword, user, projects));
        searchResult.updateSearchType();

        switch (searchResult.getSearchType()) {
            case ISSUE:
                searchResult.setIssues(finder.findIssues(keyword, user, projects, pageParam));
                break;
            case USER:
                searchResult.setUsers(finder.findUsers(keyword, pageParam));
                break;
            case PROJECT:
                searchResult.setProjects(finder.findProjects(keyword, projects, pageParam));
                break;
            case POST:
                searchResult.setPosts(finder.findPosts(keyword, user, projects, pageParam));
                break;
            case MILESTONE:
                searchResult.setMilestones(finder.findMilestones(keyword, projects, pageParam));
                break;
            case ISSUE_COMMENT:
                searchResult.setIssueComments(finder.findIssueComments(keyword, user, projects, pageParam));
                break;
            case POST_COMMENT:
                searchResult.setPostComments(finder.findPostComments(keyword, user, projects, pageParam));
                break;
            case REVIEW:
                searchResult.setReviews(finder.findReviews(keyword, user, projects, pageParam));
                break;
        }

        searchResult.updateTotalPageCount(pageParam.getSize());
        return searchResult;
    }

    public SearchResult searchInGroup(String keyword, User user, Organization organization, SearchType searchType, PageParam pageParam) {
        List<Project> projects = finder.getReadableProjects(user, organization);

        SearchResult searchResult = new SearchResult();
        searchResult.setKeyword(keyword);
        searchResult.setSearchType(searchType);
        searchResult.setIssuesCount(finder.countIssues(keyword, user, projects, organization));
        searchResult.setUsersCount(finder.countUsers(keyword, organization));
        searchResult.setProjectsCount(finder.countProjects(keyword, projects));
        searchResult.setPostsCount(finder.countPosts(keyword, user, projects, organization));
        searchResult.setMilestonesCount(finder.countMilestones(keyword, projects));
        searchResult.setIssueCommentsCount(finder.countIssueComments(keyword, user, projects, organization));
        searchResult.setPostCommentsCount(finder.countPostComments(keyword, user, projects, organization));
        searchResult.setReviewsCount(finder.countReviews(keyword, user, projects, organization));
        searchResult.updateSearchType();

        switch (searchResult.getSearchType()) {
            case ISSUE:
                searchResult.setIssues(finder.findIssues(keyword, user, projects, organization, pageParam));
                break;
            case USER:
                searchResult.setUsers(finder.findUsers(keyword, organization, pageParam));
                break;
            case PROJECT:
                searchResult.setProjects(finder.findProjects(keyword, projects, pageParam));
                break;
            case POST:
                searchResult.setPosts(finder.findPosts(keyword, user, projects, organization, pageParam));
                break;
            case MILESTONE:
                searchResult.setMilestones(finder.findMilestones(keyword, projects, pageParam));
                break;
            case ISSUE_COMMENT:
                searchResult.setIssueComments(finder.findIssueComments(keyword, user, projects, organization, pageParam));
                break;
            case POST_COMMENT:
                searchResult.setPostComments(finder.findPostComments(keyword, user, projects, organization, pageParam));
                break;
            case REVIEW:
                searchResult.setReviews(finder.findReviews(keyword, user, projects, organization, pageParam));
                break;
        }

        searchResult.updateTotalPageCount(pageParam.getSize());
        return searchResult;
    }

    public SearchResult searchInProject(String keyword, User user, Project project, SearchType searchType, PageParam pageParam) {
        boolean readAllowed = AccessControl.isAllowed(user, project.asResource(), Operation.READ);

        SearchResult searchResult = new SearchResult();
        searchResult.setSearchType(searchType);
        searchResult.setKeyword(keyword);
        searchResult.setIssuesCount(finder.countIssues(keyword, user, project, readAllowed));
        searchResult.setPostsCount(finder.countPosts(keyword, user, project, readAllowed));
        searchResult.setIssueCommentsCount(finder.countIssueComments(keyword, user, project, readAllowed));
        searchResult.setPostCommentsCount(finder.countPostComments(keyword, user, project, readAllowed));
        searchResult.setReviewsCount(finder.countReviews(keyword, user, project, readAllowed));
        if (readAllowed) {
            searchResult.setUsersCount(finder.countUsers(keyword, project));
            searchResult.setMilestonesCount(finder.countMilestones(keyword, project));
        } else {
            searchResult.setUsersCount(0);
            searchResult.setMilestonesCount(0);
        }
        searchResult.updateSearchType();

        switch (searchResult.getSearchType()) {
            case ISSUE:
                searchResult.setIssues(finder.findIssues(keyword, user, project, readAllowed, pageParam));
                break;
            case USER:
                if (readAllowed) {
                    searchResult.setUsers(finder.findUsers(keyword, project, pageParam));
                } else {
                    List<User> emptyList = Collections.emptyList();
                    searchResult.setUsers(emptyList);
                }
                break;
            case POST:
                searchResult.setPosts(finder.findPosts(keyword, user, project, readAllowed, pageParam));
                break;
            case MILESTONE:
                if (readAllowed) {
                    searchResult.setMilestones(finder.findMilestones(keyword, project, pageParam));
                } else {
                    List<Milestone> emptyList = Collections.emptyList();
                    searchResult.setMilestones(emptyList);
                }
                break;
            case ISSUE_COMMENT:
                searchResult.setIssueComments(finder.findIssueComments(keyword, user, project, readAllowed, pageParam));
                break;
            case POST_COMMENT:
                searchResult.setPostComments(finder.findPostComments(keyword, user, project, readAllowed, pageParam));
                break;
            case REVIEW:
                searchResult.setReviews(finder.findReviews(keyword, user, project, readAllowed, pageParam));
                break;
        }

        searchResult.updateTotalPageCount(pageParam.getSize());
        return searchResult;
    }

    /**
     *
     * Data Synchronization Methods
     *
     */

    public static void save(User user) {
        if (!USE_ELASTIC) {
            return;
        }

        if (synchonizable()) {
            getObject().synchronizer.index(user);
        } else {
            PendingData.add(user.id, PendingData.DataType.USER, PendingData.DataOperation.INDEX);
        }
    }

    public static void update(User user) {
        if (!USE_ELASTIC) {
            return;
        }

        if (synchonizable()) {
            getObject().synchronizer.update(user);
        } else {
            PendingData.add(user.id, PendingData.DataType.USER, PendingData.DataOperation.UPDATE);
        }
    }

    public static void delete(User user) {
        if (!USE_ELASTIC) {
            return;
        }

        if (synchonizable()) {
            getObject().synchronizer.delete(user);
        } else {

            PendingData.add(user.id, PendingData.DataType.USER, PendingData.DataOperation.DELETE);
        }
    }

    public static void save(@Nonnull Project project) {
        if (!USE_ELASTIC) {
            return;
        }

        if (synchonizable()) {
            getObject().synchronizer.index(project);
        } else {
            PendingData.add(project.id, PendingData.DataType.PROJECT, PendingData.DataOperation.INDEX);
        }
    }

    public static void update(@Nonnull Project project) {
        if (!USE_ELASTIC) {
            return;
        }

        if (synchonizable()) {
            getObject().synchronizer.update(project);
        } else {
            PendingData.add(project.id, PendingData.DataType.PROJECT, PendingData.DataOperation.UPDATE);
        }
    }

    /**
     * Delete the document of the {@code project}.
     * Delete all documents(IssueComment, Issue, PostingComment, Posting, Milestone, ReviewComment)
     * related with the {@code project}.
     * Update user documents to synchronize project's member data.
     *
     * @param pendingData
     * @param project
     */
    public static void delete(@Nonnull Project project) {
        if (!USE_ELASTIC) {
            return;
        }

        if (synchonizable()) {
            getObject().synchronizer.delete(project);
        } else {
            PendingData.add(project.id, PendingData.DataType.PROJECT, PendingData.DataOperation.DELETE);

            for (PostingComment postingComment : PostingComment.find.where().eq("posting.project", project).findList()) {
                PendingData.add(postingComment .id, PendingData.DataType.POSTING_COMMENT, PendingData.DataOperation.DELETE);
            }

            for (Posting posting: Posting.finder.where().eq("project", project).findList()) {
                PendingData.add(posting .id, PendingData.DataType.POSTING, PendingData.DataOperation.DELETE);
            }

            for (IssueComment issueComment : IssueComment.find.where().eq("issue.project", project).findList()) {
                PendingData.add(issueComment.id, PendingData.DataType.ISSUE_COMMENT, PendingData.DataOperation.DELETE);
            }

            for (Issue issue : Issue.finder.where().eq("project", project).findList()) {
                PendingData.add(issue.id, PendingData.DataType.ISSUE_COMMENT, PendingData.DataOperation.DELETE);
            }

            for (Milestone milestone: Milestone.find.where().eq("project", project).findList()) {
                PendingData.add(milestone.id, PendingData.DataType.ISSUE_COMMENT, PendingData.DataOperation.DELETE);
            }

            for (ReviewComment reviewComment: ReviewComment.find.where().eq("thread.project", project).findList()) {
                PendingData.add(reviewComment.id, PendingData.DataType.ISSUE_COMMENT, PendingData.DataOperation.DELETE);
            }

            for (User user : User.findUsersByProject(project.id)) {
                PendingData.add(user.id, PendingData.DataType.USER, PendingData.DataOperation.UPDATE);
            }


        }
    }

    private static boolean synchonizable() {
        return getObject().synchronizer.isAvailable();
    }

}
