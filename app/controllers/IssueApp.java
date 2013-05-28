package controllers;

import models.*;
import models.enumeration.*;

import play.mvc.Http;
import views.html.issue.edit;
import views.html.issue.view;
import views.html.issue.list;
import views.html.issue.create;
import views.html.error.notfound;
import views.html.error.forbidden;

import utils.AccessControl;
import utils.Callback;
import utils.JodaDateUtil;
import utils.HttpUtil;

import play.data.Form;
import play.mvc.Call;
import play.mvc.Result;

import jxl.write.WriteException;
import org.apache.tika.Tika;
import com.avaje.ebean.Page;
import com.avaje.ebean.ExpressionList;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.avaje.ebean.Expr.icontains;

public class IssueApp extends AbstractPostingApp {
    public static class SearchCondition extends AbstractPostingApp.SearchCondition {
        public String state;
        public Boolean commentedCheck;
        public Long milestoneId;
        public Set<Long> labelIds;
        public String authorLoginId;
        public Long assigneeId;

        public SearchCondition() {
            super();
            milestoneId = null;
            state = State.OPEN.name();
            commentedCheck = false;
        }

        private ExpressionList<Issue> asExpressionList(Project project) {
            ExpressionList<Issue> el = Issue.finder.where().eq("project.id", project.id);

            if (filter != null) {
                el.or(icontains("title", filter), icontains("body", filter));
            }

            if (authorLoginId != null && !authorLoginId.isEmpty()) {
                User user = User.findByLoginId(authorLoginId);
                if (!user.isAnonymous()) {
                    el.eq("authorId", user.id);
                } else {
                    List<Long> ids = new ArrayList<Long>();
                    for (User u : User.find.where().icontains("loginId", authorLoginId).findList()) {
                        ids.add(u.id);
                    }
                    el.in("authorId", ids);
                }
            }

            if (assigneeId != null) {
                el.eq("assignee.user.id", assigneeId);
                el.eq("assignee.project.id", project.id);
            }

            if (milestoneId != null) {
                el.eq("milestone.id", milestoneId);
            }

            if (labelIds != null) {
                for (Long labelId : labelIds) {
                    el.eq("labels.id", labelId);
                }
            }

            if (commentedCheck) {
                el.ge("numOfComments", AbstractPosting.NUMBER_OF_ONE_MORE_COMMENTS);
            }

            State st = State.getValue(state);
            if (st.equals(State.OPEN) || st.equals(State.CLOSED)) {
                el.eq("state", st);
            }

            if (orderBy != null) {
                el.orderBy(orderBy + " " + orderDir);
            }

            return el;
        }
    }

    /**
     * 페이지 처리된 이슈들의 리스트를 보여준다.
     *
     * @param projectName
     *            프로젝트 이름
     * @param state
     *            이슈 해결 상태
     * @return
     * @throws IOException
     * @throws WriteException
     */
    public static Result issues(String userName, String projectName, String state, String format, int pageNum) throws WriteException, IOException {
        Project project = ProjectApp.getProject(userName, projectName);

        if (!AccessControl.isAllowed(UserApp.currentUser(), project.asResource(), Operation.READ)) {
            return forbidden(views.html.error.forbidden.render(project));
        }

        Form<SearchCondition> issueParamForm = new Form<SearchCondition>(SearchCondition.class);
        SearchCondition searchCondition = issueParamForm.bindFromRequest().get();
        searchCondition.pageNum = pageNum - 1;
        searchCondition.state = state;

        String[] labelIds = request().queryString().get("labelIds");
        if (labelIds != null) {
            for (String labelId : labelIds) {
                searchCondition.labelIds.add(Long.valueOf(labelId));
            }
        }

        ExpressionList<Issue> el = searchCondition.asExpressionList(project);

        if (format.equals("xls")) {
            return issuesAsExcel(el, project);
        } else {
            Page<Issue> issues = el
                .findPagingList(ITEMS_PER_PAGE).getPage(searchCondition.pageNum);

            return ok(list.render("title.issueList", issues, searchCondition, project));
        }
    }

    public static Result issuesAsExcel(ExpressionList<Issue> el, Project project)
            throws WriteException, IOException, UnsupportedEncodingException {
        byte[] excelData = Issue.excelFrom(el.findList());
        String filename = HttpUtil.encodeContentDisposition(
                project.name + "_issues_" + JodaDateUtil.today().getTime() + ".xls");

        response().setHeader("Content-Type", new Tika().detect(filename));
        response().setHeader("Content-Disposition", "attachment; " + filename);

        return ok(excelData);
    }

    public static Result issue(String userName, String projectName, Long number) {
        Project project = ProjectApp.getProject(userName, projectName);
        Issue issueInfo = Issue.findByNumber(project, number);

        if (issueInfo == null) {
            return notFound(views.html.error.notfound.render("error.notfound", project, "issue"));
        }

        if (!AccessControl.isAllowed(UserApp.currentUser(), issueInfo.asResource(), Operation.READ)) {
            return forbidden(views.html.error.forbidden.render(project));
        }

        for (IssueLabel label: issueInfo.labels) {
            label.refresh();
        }

        Form<Comment> commentForm = new Form<Comment>(Comment.class);
        Form<Issue> editForm = new Form<Issue>(Issue.class).fill(Issue.findByNumber(project, number));

        return ok(view.render("title.issueDetail", issueInfo, editForm, commentForm, project));
    }

    public static Result newIssueForm(String userName, String projectName) {
        Project project = ProjectApp.getProject(userName, projectName);

        return newPostingForm(project, ResourceType.ISSUE_POST,
                create.render("title.newIssue", new Form<Issue>(Issue.class), project));
    }

    /**
     * 여러 이슈를 한번에 갱신하려는 요청에 응답한다.
     *
     * when: 이슈 목록 페이지에서 이슈를 체크하고 상단의 갱신 드롭박스를 이용해 체크한 이슈들을 갱신할 때
     *
     * 갱신을 시도한 이슈들 중 하나 이상 갱신에 성공했다면 이슈 목록 페이지로 리다이렉트한다. (303 See Other)
     * 어떤 이슈에 대한 갱신 요청이든 모두 실패했으며, 그 중 권한 문제로 실패한 것이 한 개 이상 있다면 403
     * Forbidden 으로 응답한다.
     * 갱신 요청이 잘못된 경우엔 400 Bad Request 로 응답한다.
     *
     * @param ownerName 프로젝트 소유자 이름
     * @param projectName 프로젝트 이름
     * @return
     * @throws IOException
     */
    public static Result massUpdate(String ownerName, String projectName) throws IOException {
        Form<IssueMassUpdate> issueMassUpdateForm
                = new Form<IssueMassUpdate>(IssueMassUpdate.class).bindFromRequest();
        if (issueMassUpdateForm.hasErrors()) {
            return badRequest(issueMassUpdateForm.errorsAsJson());
        }
        IssueMassUpdate issueMassUpdate = issueMassUpdateForm.get();

        Project project = Project.findByOwnerAndProjectName(ownerName, projectName);

        int updatedItems = 0;
        int rejectedByPermission = 0;

        for (Issue issue : issueMassUpdate.issues) {
            if (!AccessControl.isAllowed(UserApp.currentUser(), issue.asResource(),
                    Operation.UPDATE)) {
                rejectedByPermission++;
                continue;
            }

            if (issueMassUpdate.assignee != null) {
                issue.assignee = Assignee.add(issueMassUpdate.assignee.id, project.id);
            }

            if (issueMassUpdate.state != null) {
                issue.state = issueMassUpdate.state;
            }

            if (issueMassUpdate.milestone != null) {
                issue.milestone = issueMassUpdate.milestone;
            }

            issue.update();
            updatedItems++;
        }

        if (updatedItems == 0 && rejectedByPermission > 0) {
            return forbidden(views.html.error.forbidden.render(project));
        }

        return redirect(
                routes.IssueApp.issues(ownerName, projectName, "all", "html", 1));
    }

    public static Result newIssue(String ownerName, String projectName) throws IOException {
        Form<Issue> issueForm = new Form<Issue>(Issue.class).bindFromRequest();
        Project project = ProjectApp.getProject(ownerName, projectName);

        if (!AccessControl.isProjectResourceCreatable(UserApp.currentUser(), project, ResourceType.ISSUE_POST)) {
            return forbidden(views.html.error.forbidden.render(project));
        }

        if (issueForm.hasErrors()) {
            return badRequest(create.render(issueForm.errors().toString(), issueForm, project));
        }

        Issue newIssue = issueForm.get();
        newIssue.createdDate = JodaDateUtil.now();
        newIssue.setAuthor(UserApp.currentUser());
        newIssue.project = project;

        newIssue.state = State.OPEN;
        addLabels(newIssue.labels, request());

        setMilestone(issueForm, newIssue);

        newIssue.save();

        // Attach all of the files in the current user's temporary storage.
        Attachment.moveAll(UserApp.currentUser().asResource(), newIssue.asResource());

        return redirect(routes.IssueApp.issues(project.owner, project.name,
                State.OPEN.state(), "html", 1));
    }

    public static Result editIssueForm(String userName, String projectName, Long number) {
        Project project = ProjectApp.getProject(userName, projectName);
        Issue issue = Issue.findByNumber(project, number);

        if (!AccessControl.isAllowed(UserApp.currentUser(), issue.asResource(), Operation.UPDATE)) {
            return forbidden(views.html.error.forbidden.render(project));
        }

        Form<Issue> editForm = new Form<Issue>(Issue.class).fill(issue);

        return ok(edit.render("title.editIssue", editForm, issue, project));
    }

    public static Result editIssue(String userName, String projectName, Long number) throws IOException {
        Form<Issue> issueForm = new Form<Issue>(Issue.class).bindFromRequest();
        final Issue issue = issueForm.get();
        setMilestone(issueForm, issue);
        final Project project = ProjectApp.getProject(userName, projectName);
        final Issue originalIssue = Issue.findByNumber(project, number);
        Call redirectTo =
                routes.IssueApp.issues(project.owner, project.name, State.OPEN.name(), "html", 1);

        // updateIssueBeforeSave.run would be called just before this issue is saved.
        // It updates some properties only for issues, such as assignee or labels, but not for non-issues.
        Callback updateIssueBeforeSave = new Callback() {
            @Override
            public void run() {
                issue.comments = originalIssue.comments;
                addLabels(issue.labels, request());
            }
        };

        return editPosting(originalIssue, issue, issueForm, redirectTo, updateIssueBeforeSave);
    }

    private static void setMilestone(Form<Issue> issueForm, Issue issue) {
        String milestoneId = issueForm.data().get("milestoneId");
        if(milestoneId != null && !milestoneId.isEmpty()) {
            issue.milestone = Milestone.findById(Long.parseLong(milestoneId));
        }
    }

    public static Result deleteIssue(String userName, String projectName, Long number) {
        Project project = ProjectApp.getProject(userName, projectName);
        Issue issue = Issue.findByNumber(project, number);
        Call redirectTo =
            routes.IssueApp.issues(project.owner, project.name, State.OPEN.state(), "html", 1);

        return delete(issue, issue.asResource(), redirectTo);
    }

    public static Result newComment(String ownerName, String projectName, Long number) throws IOException {
        Project project = Project.findByOwnerAndProjectName(ownerName, projectName);
        final Issue issue = Issue.findByNumber(project, number);
        Call redirectTo = routes.IssueApp.issue(project.owner, project.name, number);
        Form<IssueComment> commentForm = new Form<IssueComment>(IssueComment.class)
                .bindFromRequest();

        if (commentForm.hasErrors()) {
            return badRequest(commentForm.errors().toString());
        }

        final IssueComment comment = commentForm.get();

        return newComment(comment, commentForm, redirectTo, new Callback() {
            @Override
            public void run() {
                comment.issue = issue;
            }
        });
    }

    public static Result deleteComment(String userName, String projectName, Long issueNumber,
            Long commentId) {
        Comment comment = IssueComment.find.byId(commentId);
        Project project = comment.asResource().getProject();
        Call redirectTo =
            routes.IssueApp.issue(project.owner, project.name, comment.getParent().id);

        return delete(comment, comment.asResource(), redirectTo);
    }

    public static void addLabels(Set<IssueLabel> labels, Http.Request request) {
        Http.MultipartFormData multipart = request.body().asMultipartFormData();
        Map<String, String[]> form;
        if (multipart != null) {
            form = multipart.asFormUrlEncoded();
        } else {
            form = request.body().asFormUrlEncoded();
        }
        String[] labelIds = form.get("labelIds");
        if (labelIds != null) {
            for (String labelId : labelIds) {
                labels.add(IssueLabel.finder.byId(Long.parseLong(labelId)));
            }
        }
    }
}
