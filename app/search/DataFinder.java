package search;

import com.avaje.ebean.Expr;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.Junction;
import models.*;
import models.enumeration.Operation;
import models.enumeration.ProjectScope;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import play.Logger;
import utils.AccessControl;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

/**
 * @author Keeun Baik
 */
public class DataFinder {

    TransportClient client;

    public DataFinder(TransportClient client) {
        this.client = client;
    }

    @Nonnull
    public List<Project> getReadableProjects(@Nonnull User user) {
        List<Project> list = projectEL(user).findList();
        Logger.debug("[SearchEngine] {{}} user can read {{}} projects ", user.toString(), list.size());
        return list;
    }

    @Nonnull
    public List<Project> getReadableProjects(@Nonnull User user, @Nonnull Organization organization) {
        List<Project> list = projectEL(user).eq("organization", organization).findList();
        Logger.debug("[SearchEngine] {{}} user can read {{}} projects in {{}} group.", user.toString(), list.size(), organization.name);
        return list;
    }

    public List<Project> findProjects(String keyword, List<Project> projects, PageParam pageParam) {
        return findProjects(projectSearchQuery(keyword, projects), pageParam);
    }

    public long countProjects(String keyword, List<Project> projects) {
        return count(projectSearchQuery(keyword, projects), SearchEngine.TYPE_PROJECT);
    }

    public List<Issue> findIssues(String keyword, User user, List<Project> projects, PageParam pageParam) {
        return findIssues(pageParam, issueSearchQuery(keyword, user, projects));
    }

    public List<Issue> findIssues(String keyword, User user, Project project, boolean readAllowed, PageParam pageParam) {
        return findIssues(pageParam, issueSearchQuery(keyword, user, project, readAllowed));
    }

    public List<Issue> findIssues(String keyword, User user, List<Project> projects, Organization organization, PageParam pageParam) {
        return findIssues(pageParam, issueSearchQuery(keyword, user, projects, organization));
    }

    public long countIssues(String keyword, User user, List<Project> projects) {
        return count(issueSearchQuery(keyword, user, projects), SearchEngine.TYPE_ISSUE);
    }

    public long countIssues(String keyword, User user, Project project, boolean readAllowed) {
        return count(issueSearchQuery(keyword, user, project, readAllowed), SearchEngine.TYPE_ISSUE);
    }

    public long countIssues(String keyword, User user, List<Project> projects, Organization organization) {
        return count(issueSearchQuery(keyword, user, projects, organization), SearchEngine.TYPE_ISSUE);
    }

    public List<User> findUsers(String keyword, PageParam pageParam) {
        return findUsers(userSearchQuery(keyword), pageParam);
    }

    public List<User> findUsers(String keyword, Organization organization, PageParam pageParam) {
        return findUsers(userSearchQuery(keyword, organization), pageParam);
    }

    public List<User> findUsers(String keyword, Project project, PageParam pageParam) {
        return findUsers(userSearchQuery(keyword, project), pageParam);
    }

    public List<User> findUsers(BoolQueryBuilder queryBuilder, PageParam pageParam) {
        SearchResponse response = client.prepareSearch(SearchEngine.INDEX_NAME).setTypes(SearchEngine.TYPE_USER)
                .setQuery(queryBuilder)
                .setFrom(pageParam.getFirstRow())
                .setSize(pageParam.getSize())
                .addSort("name", SortOrder.ASC)
                .execute()
                .actionGet();
        play.Logger.debug("[SearchEngine] get {{}} users from {{}} of total {{}}",
                response.getHits().getHits().length, pageParam.getFirstRow(), response.getHits().getTotalHits());
        return User.find.where().in("id", getIds(response)).orderBy().asc("name").findList();
    }

    public long countUsers(String keyword) {
        return count(userSearchQuery(keyword), SearchEngine.TYPE_USER);
    }

    public long countUsers(String keyword, Organization organization) {
        return count(userSearchQuery(keyword, organization), SearchEngine.TYPE_USER);
    }

    public long countUsers(String keyword, Project project) {
        return count(userSearchQuery(keyword, project), SearchEngine.TYPE_USER);
    }

    public List<IssueComment> findIssueComments(String keyword, User user, List<Project> projects, PageParam pageParam) {
        return findIssueComments(issueCommentSearchQuery(keyword, user, projects), pageParam);
    }

    public List<IssueComment> findIssueComments(String keyword, User user, Project project, boolean readAllowed, PageParam pageParam) {
        return findIssueComments(issueCommentSearchQuery(keyword, user, project, readAllowed), pageParam);
    }

    public List<IssueComment> findIssueComments(String keyword, User user, List<Project> projects,
                                                Organization organization, PageParam pageParam) {
        return findIssueComments(issueCommentSearchQuery(keyword, user, projects, organization), pageParam);
    }

    public long countIssueComments(String keyword, User user, List<Project> projects) {
        return count(issueCommentSearchQuery(keyword, user, projects), SearchEngine.TYPE_ISSUE_COMMENT);
    }

    public long countIssueComments(String keyword, User user, Project project, boolean readAllowed) {
        return count(issueCommentSearchQuery(keyword, user, project, readAllowed), SearchEngine.TYPE_ISSUE_COMMENT);
    }

    public long countIssueComments(String keyword, User user, List<Project> projects, Organization organization) {
        return count(issueCommentSearchQuery(keyword, user, projects, organization), SearchEngine.TYPE_ISSUE_COMMENT);
    }

    public long countPosts(String keyword, User user, List<Project> projects) {
        return count(postSearchQuery(keyword, user, projects), SearchEngine.TYPE_POST);
    }

    public long countPosts(String keyword, User user, Project project, boolean readAllowed) {
        return count(postSearchQuery(keyword, user, project, readAllowed), SearchEngine.TYPE_POST);
    }

    public long countPosts(String keyword, User user, List<Project> projects, Organization organization) {
        return count(postSearchQuery(keyword, user, projects, organization), SearchEngine.TYPE_POST);
    }

    public List<Posting> findPosts(String keyword, User user, List<Project> projects, PageParam pageParam) {
        return findPosts(postSearchQuery(keyword, user, projects), pageParam);
    }

    public List<Posting> findPosts(String keyword, User user, Project project, boolean readAllowed, PageParam pageParam) {
        return findPosts(postSearchQuery(keyword, user, project, readAllowed), pageParam);
    }

    public List<Posting> findPosts(String keyword, User user, List<Project> projects, Organization organization, PageParam pageParam) {
        return findPosts(postSearchQuery(keyword, user, projects, organization), pageParam);
    }

    public long countMilestones(String keyword, List<Project> projects) {
        return count(milestoneSearchQuery(keyword, projects), SearchEngine.TYPE_MILESTONE);
    }

    public long countMilestones(String keyword, Project project) {
        return count(milestoneSearchQuery(keyword, project), SearchEngine.TYPE_MILESTONE);
    }

    public List<Milestone> findMilestones(String keyword, List<Project> projects, PageParam pageParam) {
        return findMilestones(milestoneSearchQuery(keyword, projects), pageParam);
    }

    public List<Milestone> findMilestones(String keyword, Project project, PageParam pageParam) {
        return findMilestones(milestoneSearchQuery(keyword, project), pageParam);
    }

    public long countPostComments(String keyword, User user, List<Project> projects) {
        return count(postingCommentSearchQuery(keyword, user, projects), SearchEngine.TYPE_POSTING_COMMENT);
    }

    public long countPostComments(String keyword, User user, Project project, boolean readAllowed) {
        return count(postingCommentSearchQuery(keyword, user, project, readAllowed), SearchEngine.TYPE_POSTING_COMMENT);
    }

    public long countPostComments(String keyword, User user, List<Project> projects, Organization organization) {
        return count(postingCommentSearchQuery(keyword, user, projects, organization), SearchEngine.TYPE_POSTING_COMMENT);
    }

    public List<PostingComment> findPostComments(String keyword, User user, List<Project> projects, PageParam pageParam) {
        return findPostComments(postingCommentSearchQuery(keyword, user, projects), pageParam);
    }

    public List<PostingComment> findPostComments(String keyword, User user, Project project, boolean readAllowed, PageParam pageParam) {
        return findPostComments(postingCommentSearchQuery(keyword, user, project, readAllowed), pageParam);
    }

    public List<PostingComment> findPostComments(String keyword, User user, List<Project> projects, Organization organization, PageParam pageParam) {
        return findPostComments(postingCommentSearchQuery(keyword, user, projects, organization), pageParam);
    }

    public long countReviews(String keyword, User user, List<Project> projects) {
        return count(reviewSearchQuery(keyword, user, projects), SearchEngine.TYPE_REVIEW);
    }

    public long countReviews(String keyword, User user, List<Project> projects, Organization organization) {
        return count(reviewSearchQuery(keyword, user, projects, organization), SearchEngine.TYPE_REVIEW);
    }

    public long countReviews(String keyword, User user, Project project, boolean readAllowed) {
        return count(reviewSearchQuery(keyword, user, project, readAllowed), SearchEngine.TYPE_REVIEW);
    }

    public List<ReviewComment> findReviews(String keyword, User user, List<Project> projects, PageParam pageParam) {
        return findReviews(reviewSearchQuery(keyword, user, projects), pageParam);
    }

    public List<ReviewComment> findReviews(String keyword, User user, Project project, boolean readAllowed, PageParam pageParam) {
        return findReviews(reviewSearchQuery(keyword, user, project, readAllowed), pageParam);
    }

    public List<ReviewComment> findReviews(String keyword, User user, List<Project> projects,
                                           Organization organization, PageParam pageParam) {
        return findReviews(reviewSearchQuery(keyword, user, projects, organization), pageParam);
    }

    @Nonnull
    private ExpressionList<Project> projectEL(@Nonnull User user) {
        ExpressionList<Project> el = Project.find.where();
        if(user.isAnonymous()) {
            el.eq("projectScope", ProjectScope.PUBLIC);
        } else {
            Junction<Project> pj = el.disjunction();
            pj.add(Expr.eq("projectScope", ProjectScope.PUBLIC)); // public
            List<Organization> orgs = Organization.findOrganizationsByUserLoginId(user.loginId); // protected
            if(!orgs.isEmpty()) {
                pj.and(Expr.in("organization", orgs), Expr.eq("projectScope", ProjectScope.PROTECTED));
            }
            pj.add(Expr.eq("projectUser.user.id", user.id)); // private
            pj.endJunction();
        }
        return el;
    }

    private List<Issue> findIssues(PageParam pageParam, BoolQueryBuilder queryBuilder) {
        SearchResponse response = client.prepareSearch(SearchEngine.INDEX_NAME)
                .setTypes(SearchEngine.TYPE_ISSUE)
                .setQuery(queryBuilder)
                .setFrom(pageParam.getPage() * pageParam.getSize())
                .setSize(pageParam.getSize())
                .addSort("created", SortOrder.DESC)
                .execute()
                .actionGet();
        play.Logger.debug("[SearchEngine] get {{}} issues from {{}} of total {{}}",
                response.getHits().getHits().length, pageParam.getFirstRow(), response.getHits().getTotalHits());
        return Issue.finder.where().in("id", getIds(response)).orderBy().desc("createdDate").findList();
    }

    private List<Project> findProjects(QueryBuilder queryBuilder, PageParam pageParam) {
        SearchResponse response = client.prepareSearch(SearchEngine.INDEX_NAME).setTypes(SearchEngine.TYPE_PROJECT)
                .setQuery(queryBuilder)
                .setFrom(pageParam.getFirstRow())
                .setSize(pageParam.getSize())
                .addSort("name", SortOrder.ASC)
                .execute()
                .actionGet();
        play.Logger.debug("[SearchEngine] get {{}} projects from {{}} of total {{}}",
                response.getHits().getHits().length, pageParam.getFirstRow(), response.getHits().getTotalHits());
        return Project.find.where().in("id", getIds(response)).orderBy().asc("name").findList();
    }

    private List<Posting> findPosts(QueryBuilder queryBuilder, PageParam pageParam) {
        SearchResponse response = client.prepareSearch(SearchEngine.INDEX_NAME).setTypes(SearchEngine.TYPE_POST)
                .setQuery(queryBuilder)
                .setFrom(pageParam.getFirstRow())
                .setSize(pageParam.getSize())
                .addSort("created", SortOrder.DESC)
                .execute()
                .actionGet();
        play.Logger.debug("[SearchEngine] get {{}} posts from {{}} of total {{}}",
                response.getHits().getHits().length, pageParam.getFirstRow(), response.getHits().getTotalHits());
        return Posting.finder.where().in("id", getIds(response)).orderBy().desc("createdDate").findList();
    }

    private List<IssueComment> findIssueComments(QueryBuilder queryBuilder, PageParam pageParam) {
        SearchResponse response = client.prepareSearch(SearchEngine.INDEX_NAME).setTypes(SearchEngine.TYPE_ISSUE_COMMENT)
                .setQuery(queryBuilder)
                .setFrom(pageParam.getFirstRow())
                .setSize(pageParam.getSize())
                .addSort("createdDate", SortOrder.DESC)
                .execute()
                .actionGet();
        play.Logger.debug("[SearchEngine] get {{}} issue comments from {{}} of total {{}}",
                response.getHits().getHits().length, pageParam.getFirstRow(), response.getHits().getTotalHits());
        return IssueComment.find.where().in("id", getIds(response)).orderBy().desc("createdDate").findList();
    }

    private List<PostingComment> findPostComments(QueryBuilder queryBuilder, PageParam pageParam) {
        SearchResponse response = client.prepareSearch(SearchEngine.INDEX_NAME).setTypes(SearchEngine.TYPE_POSTING_COMMENT)
                .setQuery(queryBuilder)
                .setFrom(pageParam.getFirstRow())
                .setSize(pageParam.getSize())
                .addSort("createdDate", SortOrder.DESC)
                .execute()
                .actionGet();
        play.Logger.debug("[SearchEngine] get {{}} post comments from {{}} of total {{}}",
                response.getHits().getHits().length, pageParam.getFirstRow(), response.getHits().getTotalHits());
        return PostingComment.find.where().in("id", getIds(response)).orderBy().desc("createdDate").findList();
    }

    private List<ReviewComment> findReviews(QueryBuilder queryBuilder, PageParam pageParam) {
        SearchResponse response = client.prepareSearch(SearchEngine.INDEX_NAME).setTypes(SearchEngine.TYPE_REVIEW)
                .setQuery(queryBuilder)
                .setFrom(pageParam.getFirstRow())
                .setSize(pageParam.getSize())
                .addSort("createdDate", SortOrder.DESC)
                .execute()
                .actionGet();
        play.Logger.debug("[SearchEngine] get {{}} reviews from {{}} of total {{}}",
                response.getHits().getHits().length, pageParam.getFirstRow(), response.getHits().getTotalHits());
        return ReviewComment.find.where().in("id", getIds(response)).orderBy().desc("createdDate").findList();
    }

    private List<Milestone> findMilestones(QueryBuilder queryBuilder, PageParam pageParam) {
        SearchResponse response = client.prepareSearch(SearchEngine.INDEX_NAME).setTypes(SearchEngine.TYPE_MILESTONE)
                .setQuery(queryBuilder)
                .setFrom(pageParam.getFirstRow())
                .setSize(pageParam.getSize())
                .addSort("dueDate", SortOrder.DESC)
                .execute()
                .actionGet();
        play.Logger.debug("[SearchEngine] get {{}} milestones from {{}} of total {{}}",
                response.getHits().getHits().length, pageParam.getFirstRow(), response.getHits().getTotalHits());
        return Milestone.find.where().in("id", getIds(response)).orderBy().desc("dueDate").findList();
    }

    private BoolQueryBuilder issueSearchQuery(String keyword, User user, List<Project> projects) {
        MultiMatchQueryBuilder matchQuery = multiMatchQuery(keyword, "title", "body", "labels");
        return boolQuery()
            .should(boolQuery().must(matchAuthor(user)).must(matchQuery))
            .should(boolQuery().must(matchAssignee(user)).must(matchQuery))
            .should(boolQuery().must(inProjects(projects)).must(matchQuery));
    }

    private BoolQueryBuilder issueSearchQuery(String keyword, User user, Project project, boolean readAllowed) {
        MultiMatchQueryBuilder matchQuery = multiMatchQuery(keyword, "title", "body", "labels");
        if (readAllowed) {
            return boolQuery().should(boolQuery().must(inProject(project)).must(matchQuery));
        } else {
            return boolQuery()
                .should(boolQuery().must(inProject(project)).must(matchAuthor(user)).must(matchQuery))
                .should(boolQuery().must(inProject(project)).must(matchAssignee(user)).must(matchQuery));
        }
    }

    private BoolQueryBuilder issueSearchQuery(String keyword, User user, List<Project> projects, Organization organization) {
        List<Project> orgProjects = organization.projects;
        MultiMatchQueryBuilder matchQuery = multiMatchQuery(keyword, "title", "contents", "labels");
        return boolQuery()
            .should(boolQuery().must(inProjects(projects)).must(matchQuery))
            .should(boolQuery().must(inProjects(orgProjects)).must(matchAuthor(user)).must(matchQuery))
            .should(boolQuery().must(inProjects(orgProjects)).must(matchAssignee(user)).must(matchQuery));
    }

    private long count(BoolQueryBuilder queryBuilder, String type) {
        CountResponse response = client.prepareCount(SearchEngine.INDEX_NAME).setTypes(type)
                .setQuery(queryBuilder)
                .execute()
                .actionGet();
        long count = response.getCount();
        play.Logger.debug("[SearchEngine] count {{}} {{}}", count, type);
        return count;
    }

    private MatchQueryBuilder matchAssignee(User user) {
        return matchQuery("assigneeUserId", user.id);
    }

    private static MatchQueryBuilder matchAuthor(User user) {
        return matchQuery("authorId", user.id);
    }

    private TermsQueryBuilder inProjects(List<Project> projects) {
        return termsQuery("projectId", longProjectIds(projects));
    }

    private MatchQueryBuilder inProject(Project project) {
        return matchQuery("projectId", project.id);
    }

    private List<Long> longProjectIds(List<Project> projects) {
        List<Long> ids= new ArrayList<>();
        for(Project project : projects) {
            ids.add(project.id);
        }
        return ids;
    }

    private List<Long> getIds(SearchResponse searchResponse) {
        List<Long> ids = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits()) {
            ids.add(Long.parseLong(hit.id()));
        }
        return ids;
    }

    private BoolQueryBuilder projectSearchQuery(String keyword, List<Project> projects) {
        return boolQuery()
                .must(idsQuery(SearchEngine.TYPE_PROJECT).ids(stringProjectIds(projects)))
                .must(multiMatchQuery(keyword, "labels", "overview", "name"));
    }

    private String[] stringProjectIds(List<Project> projects) {
        List<String> idList = new ArrayList<>();
        for(Project project : projects) {
            idList.add(project.id.toString());
        }

        return idList.toArray(new String[idList.size()]);
    }

    private BoolQueryBuilder userSearchQuery(String keyword) {
        return boolQuery()
                .must(matchQuery("state", "ACTIVE"))
                .must(multiMatchQuery(keyword, "name", "loginId"));
    }

    private BoolQueryBuilder userSearchQuery(String keyword, Organization organization) {
        return userSearchQuery(keyword).must(matchQuery("groups", organization.id));
    }

    private BoolQueryBuilder userSearchQuery(String keyword, Project project) {
        return userSearchQuery(keyword).must(matchQuery("projects", project.id));
    }

    private BoolQueryBuilder issueCommentSearchQuery(String keyword, User user, List<Project> projects) {
        MatchQueryBuilder matchQuery = matchQuery("contents", keyword);
        return boolQuery()
                .should(boolQuery().must(inProjects(projects)).must(matchQuery))
                .should(boolQuery().must(matchAuthor(user)).must(matchQuery))
                .should(boolQuery().must(matchQuery("issueAuthorId", user.id)).must(matchQuery))
                .should(boolQuery().must(matchQuery("issueAssigneeUserId", user.id)).must(matchQuery));
    }

    private BoolQueryBuilder issueCommentSearchQuery(String keyword, User user, Project project, boolean readAllowed) {
        MatchQueryBuilder matchQuery = matchQuery("contents", keyword);
        if (readAllowed) {
            return boolQuery().must(inProject(project)).must(matchQuery);
        } else {
            return boolQuery()
                .should(boolQuery().must(inProject(project)).must(matchAuthor(user)).must(matchQuery))
                .should(boolQuery().must(inProject(project)).must(matchQuery("issueAuthorId", user.id)).must(matchQuery))
                .should(boolQuery().must(inProject(project)).must(matchQuery("issueAssigneeUserId", user.id)).must(matchQuery));
        }
    }

    private BoolQueryBuilder issueCommentSearchQuery(String keyword, User user, List<Project> projects, Organization organization) {
        MatchQueryBuilder matchQuery = matchQuery("contents", keyword);
        List<Project> orgProjects = organization.projects;
        return boolQuery()
                .should(boolQuery().must(inProjects(projects)).must(matchQuery))
                .should(boolQuery().must(inProjects(orgProjects)).must(matchAuthor(user)).must(matchQuery))
                .should(boolQuery().must(inProjects(orgProjects)).must(matchQuery("issueAuthorId", user.id)).must(matchQuery))
                .should(boolQuery().must(inProjects(orgProjects)).must(matchQuery("issueAssigneeUserId", user.id)).must(matchQuery));
    }

    private BoolQueryBuilder postSearchQuery(String keyword, User user, List<Project> projects) {
        MultiMatchQueryBuilder matchQuery = multiMatchQuery(keyword, "title", "body");
        return boolQuery()
                .should(boolQuery().must(inProjects(projects)).must(matchQuery))
                .should(boolQuery().must(matchAuthor(user)).must(matchQuery));
    }

    private BoolQueryBuilder postSearchQuery(String keyword, User user, Project project, boolean readAllowed) {
        MultiMatchQueryBuilder matchQuery = multiMatchQuery(keyword, "title", "body");
        if (readAllowed) {
            return boolQuery().must(inProject(project)).must(matchQuery);
        } else {
            return boolQuery().must(inProject(project)).must(matchAuthor(user)).must(matchQuery);
        }
    }

    private BoolQueryBuilder postSearchQuery(String keyword, User user, List<Project> projects, Organization organization) {
        MultiMatchQueryBuilder matchQuery = multiMatchQuery(keyword, "title", "body");
        return boolQuery()
                .should(boolQuery().must(inProjects(projects)).must(matchQuery))
                .should(boolQuery().must(inProjects(organization.projects)).must(matchAuthor(user)).must(matchQuery));
    }

    private BoolQueryBuilder milestoneSearchQuery(String keyword, List<Project> projects) {
        return boolQuery()
                .must(inProjects(projects))
                .must(multiMatchQuery(keyword, "title", "contents"));
    }

    private BoolQueryBuilder milestoneSearchQuery(String keyword, Project project) {
        return boolQuery()
                .must(inProject(project))
                .must(multiMatchQuery(keyword, "title", "contents"));
    }

    private BoolQueryBuilder reviewSearchQuery(String keyword, User user, List<Project> projects) {
        MatchQueryBuilder matchQuery = matchQuery("contents", keyword);
        return boolQuery()
                .should(boolQuery().must(inProjects(projects)).must(matchQuery))
                .should(boolQuery().must(matchAuthor(user)).must(matchQuery))
                .should(boolQuery().must(matchQuery("reviewAuthorId", user.id)).must(matchQuery));
    }

    private BoolQueryBuilder reviewSearchQuery(String keyword, User user, List<Project> projects, Organization organization) {
        MatchQueryBuilder matchQuery = matchQuery("contents", keyword);
        List<Project> orgProjects = organization.projects;
        return boolQuery()
                .should(boolQuery().must(inProjects(projects)).must(matchQuery))
                .should(boolQuery().must(inProjects(orgProjects)).must(matchAuthor(user)).must(matchQuery))
                .should(boolQuery().must(inProjects(orgProjects)).must(matchQuery("reviewAuthorId", user.id)).must(matchQuery));
    }

    private BoolQueryBuilder reviewSearchQuery(String keyword, User user, Project project, boolean readAllowed) {
        MatchQueryBuilder matchQuery = matchQuery("contents", keyword);
        if (readAllowed) {
            return boolQuery().must(inProject(project)).must(matchQuery);
        } else {
            return boolQuery()
                .should(boolQuery().must(inProject(project)).must(matchAuthor(user)).must(matchQuery))
                .should(boolQuery().must(inProject(project)).must(matchQuery("reviewAuthorId", user.id)).must(matchQuery));
        }
    }

    private BoolQueryBuilder postingCommentSearchQuery(String keyword, User user, List<Project> projects) {
        MatchQueryBuilder matchQuery = matchQuery("contents", keyword);
        return boolQuery()
                .should(boolQuery().must(inProjects(projects)).must(matchQuery))
                .should(boolQuery().must(matchAuthor(user)).must(matchQuery))
                .should(boolQuery().must(matchQuery("postingAuthorId", user.id)).must(matchQuery));
    }

    private BoolQueryBuilder postingCommentSearchQuery(String keyword, User user, Project project, boolean readAllowed) {
        MatchQueryBuilder matchQuery = matchQuery("contents", keyword);
        if (readAllowed) {
            return boolQuery().must(inProject(project)).must(matchQuery);
        } else {
            return boolQuery()
                .should(boolQuery().must(inProject(project)).must(matchAuthor(user)).must(matchQuery))
                .should(boolQuery().must(inProject(project)).must(matchQuery("postingAuthorId", user.id)).must(matchQuery));
        }
    }

    private BoolQueryBuilder postingCommentSearchQuery(String keyword, User user,
                                                       List<Project> projects, Organization organization) {
        MatchQueryBuilder matchQuery = matchQuery("contents", keyword);
        List<Project> orgProjects = organization.projects;
        return boolQuery()
                .should(boolQuery().must(inProjects(projects)).must(matchQuery))
                .should(boolQuery().must(inProjects(orgProjects)).must(matchAuthor(user)).must(matchQuery))
                .should(boolQuery().must(inProjects(orgProjects)).must(matchQuery("postingAuthorId", user.id)).must(matchQuery));
    }

}
