package controllers;

import com.avaje.ebean.Page;
import com.avaje.ebean.ExpressionList;
import models.*;
import models.enumeration.Operation;
import models.enumeration.RoleType;
import models.enumeration.Direction;
import models.enumeration.Matching;
import models.support.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.tmatesoft.svn.core.SVNException;
import play.data.Form;
import play.db.ebean.Transactional;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import playRepository.Commit;
import playRepository.PlayRepository;
import playRepository.RepositoryService;
import utils.AccessControl;
import utils.Constants;
import utils.HttpUtil;
import views.html.project.*;
import play.i18n.Messages;

import javax.servlet.ServletException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static play.data.Form.form;
import static play.libs.Json.toJson;
import static com.avaje.ebean.Expr.contains;

public class ProjectApp extends Controller {
    public static final String[] LOGO_TYPE = {"jpg", "jpeg", "png", "gif", "bmp"};
    private static final int MAX_FETCH_PROJECTS = 1000;

    /**
     * getProject
     * @param userName
     * @param projectName
     * @return
     */
    public static Project getProject(String userName, String projectName) {
        return Project.findByNameAndOwner(userName, projectName);
    }

    /**
     * project
     * @param userName
     * @param projectName
     * @return
     * @throws IOException
     * @throws ServletException
     * @throws SVNException
     * @throws GitAPIException
     */
    public static Result project(String userName, String projectName) throws IOException, ServletException, SVNException, GitAPIException {
        Project project = ProjectApp.getProject(userName, projectName);
        if (!AccessControl.isAllowed(UserApp.currentUser(), project.asResource(), Operation.READ)) {
            return unauthorized(views.html.project.unauthorized.render(project));
        }

        PlayRepository repository = RepositoryService.getRepository(project);

        List<Commit> commits = null;
        try {
            commits = repository.getHistory(1, 5, null);
        } catch (NoHeadException e) { }
        List<Issue> issues = Issue.findRecentlyCreated(project, 5);
        List<Posting> postings = Posting.findRecentlyUpdated(project, 5);

        List<History> histories = History.makeHistory(userName, project, commits, issues, postings);

        return ok(overview.render("title.projectHome", getProject(userName, projectName), histories));
    }

    /**
     * newProjectForm
     * @return
     */
    public static Result newProjectForm() {
        if (session().get(UserApp.SESSION_USERID) == null) {
            flash(Constants.WARNING, "user.login.alert");
            return redirect(routes.UserApp.loginForm());
        } else
            return ok(create.render("title.newProject", form(Project.class)));
    }

    /**
     * settingForm
     * @param userName
     * @param projectName
     * @return
     */
    public static Result settingForm(String userName, String projectName) {
        Project project = getProject(userName, projectName);
        if (!AccessControl.isAllowed(UserApp.currentUser(), project.asResource(), Operation.UPDATE)) {
            return unauthorized(views.html.project.unauthorized.render(project));
        }

        Form<Project> projectForm = form(Project.class).fill(project);
        return ok(setting.render("title.projectSetting", projectForm, project));
    }

    @Transactional
    public static Result newProject() throws Exception {
        Form<Project> filledNewProjectForm = form(Project.class).bindFromRequest();
        if (Project.isProject(UserApp.currentUser().loginId,
                filledNewProjectForm.field("name").value())) {
            flash(Constants.WARNING, "project.name.duplicate");
            filledNewProjectForm.reject("name");
            return badRequest(create.render("title.newProject", filledNewProjectForm));
        } else if (filledNewProjectForm.hasErrors()) {
            System.out.println("=====" + filledNewProjectForm.errorsAsJson());
            filledNewProjectForm.reject("name");
            flash(Constants.WARNING, "project.name.alert");
            return badRequest(create.render("title.newProject", filledNewProjectForm));
        } else {
            Project project = filledNewProjectForm.get();
            project.owner = UserApp.currentUser().loginId;
            ProjectUser.assignRole(UserApp.currentUser().id,
                    Project.create(project), RoleType.MANAGER);

            RepositoryService.createRepository(project);

            return redirect(routes.ProjectApp.project(project.owner, project.name));
        }
    }

    /**
     * settingProject
     * @param userName
     * @param projectName
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static Result settingProject(String userName, String projectName) throws IOException, NoSuchAlgorithmException {
        Form<Project> filledUpdatedProjectForm = form(Project.class)
                .bindFromRequest();
        Project project = filledUpdatedProjectForm.get();

        if (!AccessControl.isAllowed(UserApp.currentUser(), project.asResource(), Operation.UPDATE)) {
            flash(Constants.WARNING, "project.member.isManager");
            return redirect(routes.ProjectApp.settingForm(userName, project.name));
        }

        if (!Project.projectNameChangeable(project.id, userName, project.name)) {
            flash(Constants.WARNING, "project.name.duplicate");
            filledUpdatedProjectForm.reject("name");
        }

        MultipartFormData body = request().body().asMultipartFormData();
        FilePart filePart = body.getFile("logoPath");

        if (filePart != null && filePart.getFilename() != null
                && filePart.getFilename().length() > 0) {
            if(!isImageFile(filePart.getFilename())) {
                flash(Constants.WARNING, "project.logo.alert");
                filledUpdatedProjectForm.reject("logoPath");
            } else if (filePart.getFile().length() > 1048576) {
                flash(Constants.WARNING, "project.logo.fileSizeAlert");
                filledUpdatedProjectForm.reject("logoPath");
            } else {
                Attachment.deleteAll(project.asResource());
                new Attachment().store(filePart.getFile(), filePart.getFilename(), project.asResource());
            }
        }

        if (filledUpdatedProjectForm.hasErrors()) {
            return badRequest(setting.render("title.projectSetting",
                    filledUpdatedProjectForm, Project.find.byId(project.id)));
        } else {
            project.update();
            return redirect(routes.ProjectApp.settingForm(userName, project.name));
        }
    }

    /**
     * isImageFile
     * @param filename
     * @return
     */
    public static boolean isImageFile(String filename) {
        boolean isImageFile = false;
        for(String suffix : LOGO_TYPE) {
            if(filename.toLowerCase().endsWith(suffix))
                isImageFile=true;
        }
        return isImageFile;
    }

    /**
     * deleteForm
     * @param userName
     * @param projectName
     * @return
     */
    public static Result deleteForm(String userName, String projectName) {
        Project project = getProject(userName, projectName);
        if (!AccessControl.isAllowed(UserApp.currentUser(), project.asResource(), Operation.UPDATE)) {
            return unauthorized(views.html.project.unauthorized.render(project));
        }

        Form<Project> projectForm = form(Project.class).fill(project);
        return ok(delete.render("title.projectSetting", projectForm, project));
    }
    
    /**
     * deleteProject
     * @param userName
     * @param projectName
     * @return
     * @throws Exception
     */
    public static Result deleteProject(String userName, String projectName) throws Exception {
        Project project = getProject(userName, projectName);

        if (AccessControl.isAllowed(UserApp.currentUser(), project.asResource(), Operation.DELETE)) {
            RepositoryService.deleteRepository(userName, projectName, project.vcs);
            project.delete();
            return redirect(routes.Application.index());
        } else {
            flash(Constants.WARNING, "project.member.isManager");
            return redirect(routes.ProjectApp.settingForm(userName, projectName));
        }
    }

    /**
     * members
     * @param userName
     * @param projectName
     * @return
     */
    public static Result members(String userName, String projectName) {
        Project project = getProject(userName, projectName);
        return ok(views.html.project.members.render("title.memberList",
                ProjectUser.findMemberListByProject(project.id), project,
                Role.getActiveRoles()));
    }

    /**
     * newMember
     * @param userName
     * @param projectName
     * @return
     */
    public static Result newMember(String userName, String projectName) {
        // TODO change into view validation
        Form<User> addMemberForm = form(User.class).bindFromRequest();
        if (addMemberForm.hasErrors()){
            flash(Constants.WARNING, "project.member.notExist");
            return redirect(routes.ProjectApp.members(userName, projectName));
        }

        User user = User
                .findByLoginId(form(User.class).bindFromRequest().get().loginId);
        Project project = getProject(userName, projectName);

        if (!AccessControl.isAllowed(UserApp.currentUser(), project.asResource(), Operation.UPDATE)) {
            flash(Constants.WARNING, "project.member.isManager");
            return redirect(routes.ProjectApp.members(userName, projectName));
        } else if (user == null) {
            flash(Constants.WARNING, "project.member.notExist");
            return redirect(routes.ProjectApp.members(userName, projectName));
        } else if (!ProjectUser.isMember(user.id, project.id)){
            ProjectUser.assignRole(user.id, project.id, RoleType.MEMBER);
        } else{
            flash(Constants.WARNING, "project.member.alreadyMember");
        }
        return redirect(routes.ProjectApp.members(userName, projectName));
    }

    /**
     * deleteMember
     * @param userName
     * @param projectName
     * @param userId
     * @return
     */
    public static Result deleteMember(String userName, String projectName,
            Long userId) {
        Project project = getProject(userName, projectName);
        if (UserApp.currentUser().id == userId
                || AccessControl.isAllowed(UserApp.currentUser(), project.asResource(), Operation.UPDATE)) {
            if (project.isOwner(User.find.byId(userId))) {
                return forbidden(Messages.get("project.member.ownerCannotLeave"));
            }
            ProjectUser.delete(userId, project.id);
            return redirect(routes.ProjectApp.members(userName, projectName));
        } else {
            return forbidden(views.html.project.unauthorized.render(project));
        }
    }

    /**
     * editMemeber
     * @param userName
     * @param projectName
     * @param userId
     * @return
     */
    public static Result editMember(String userName, String projectName, Long userId) {
        Project project = getProject(userName, projectName);
        if (AccessControl.isAllowed(UserApp.currentUser(), project.asResource(), Operation.UPDATE)) {
            if (project.isOwner(User.find.byId(userId))) {
                return forbidden(Messages.get("project.member.ownerMustBeAManager"));
            }
            ProjectUser.assignRole(userId, project.id, form(Role.class)
                    .bindFromRequest().get().id);
            return status(Http.Status.NO_CONTENT);
        } else {
            return forbidden(Messages.get("project.member.isManager"));
        }
    }

    /**
     * projects
     * @param filter
     * @param state
     * @param pageNum
     * @return
     */
    public static Result projects(String filter, String state, int pageNum) {
        final String HTML = "text/html";
        final String JSON = "application/json";
        String prefer = HttpUtil.getPreferType(request(), JSON, HTML);

        if (prefer == null) {
            return status(Http.Status.NOT_ACCEPTABLE);
        }

        if (prefer.equals(JSON)) {
            String query = request().getQueryString("query");
            List<String> projectNames = new ArrayList<String>();
            ExpressionList<Project> el = Project.find.where().or(contains("name", query), contains("owner", query));
            int total = el.findRowCount();
            if (total > MAX_FETCH_PROJECTS) {
                el.setMaxRows(MAX_FETCH_PROJECTS);
                response().setHeader("Content-Range", "items " + MAX_FETCH_PROJECTS + "/" + total);
            }
            for (Project project: el.findList()) {
                projectNames.add(project.owner + "/" + project.name);
            }

            return ok(toJson(projectNames));
        }

        OrderParams orderParams = new OrderParams();
        SearchParams searchParams = new SearchParams();

        orderParams.add("createdDate", Direction.DESC);
        searchParams.add("name", filter, Matching.CONTAINS);
        if (state.toLowerCase().equals("public")) {
            searchParams.add("share_option", true, Matching.EQUALS);
        } else if (state.toLowerCase().equals("private")) {
            searchParams.add("share_option", false, Matching.EQUALS);
        }

        Page<Project> projects = FinderTemplate.getPage(
                orderParams, searchParams, Project.find, Project.PROJECT_COUNT_PER_PAGE, pageNum - 1);

        return ok(views.html.project.list.render("title.projectList", projects, filter, state));
    }

    /**
     * tags
     * @param ownerName
     * @param projectName
     * @return
     */
    public static Result tags(String ownerName, String projectName) {
        Project project = Project.findByNameAndOwner(ownerName, projectName);
        if (!AccessControl.isAllowed(UserApp.currentUser(), project.asResource(), Operation.READ)) {
            return forbidden();
        }

        if (!request().accepts("application/json")) {
            return status(Http.Status.NOT_ACCEPTABLE);
        }

        Map<Long, String> tags = new HashMap<Long, String>();
        for (Tag tag: project.tags) {
            tags.put(tag.id, tag.toString());
        }

        return ok(toJson(tags));
    }

    /**
     * tag
     * @param ownerName
     * @param projectName
     * @return
     */
    public static Result tag(String ownerName, String projectName) {
        Project project = Project.findByNameAndOwner(ownerName, projectName);
        if (!AccessControl.isAllowed(UserApp.currentUser(), project.tagsAsResource(), Operation.UPDATE)) {
            return forbidden();
        }

        // Get category and name from the request. Return 400 Bad Request if name is not given.
        Map<String, String[]> data = request().body().asFormUrlEncoded();
        String category = HttpUtil.getFirstValueFromQuery(data, "category");
        String name = HttpUtil.getFirstValueFromQuery(data, "name");
        if (name == null || name.length() == 0) {
            // A tag must have its name.
            return badRequest("Tag name is missing.");
        }

        Tag tag = Tag.find
            .where().eq("category", category).eq("name", name).findUnique();

        boolean isCreated = false;
        if (tag == null) {
            // Create new tag if there is no tag which has the given name.
            tag = new Tag(category, name);
            tag.save();
            isCreated = true;
        }

        Boolean isAttached = project.tag(tag);

        if (!isCreated && !isAttached) {
            // Something is wrong. This case is not possible.
            play.Logger.warn(
                    "A tag '" + tag + "' is created but failed to attach to project '"
                    + project + "'.");
        }

        if (isAttached) {
            // Return the attached tag. The return type is Map<Long, String>
            // even if there is only one tag, to unify the return type with
            // ProjectApp.tags().
            Map<Long, String> tags = new HashMap<Long, String>();
            tags.put(tag.id, tag.toString());
            if (isCreated) {
                return created(toJson(tags));
            } else {
                return ok(toJson(tags));
            }
        } else {
            // Return 204 No Content if the tag has been attached already.
            return status(Http.Status.NO_CONTENT);
        }
    }

    /**
     * untag
     * @param ownerName
     * @param projectName
     * @param id
     * @return
     */
    public static Result untag(String ownerName, String projectName, Long id) {
        Project project = Project.findByNameAndOwner(ownerName, projectName);
        if (!AccessControl.isAllowed(UserApp.currentUser(), project.tagsAsResource(), Operation.UPDATE)) {
            return forbidden();
        }

        // _method must be 'delete'
        Map<String, String[]> data = request().body().asFormUrlEncoded();
        if (!HttpUtil.getFirstValueFromQuery(data, "_method").toLowerCase()
                .equals("delete")) {
            return badRequest("_method must be 'delete'.");
        }

        Tag tag = Tag.find.byId(id);

        if (tag == null) {
            return notFound();
        }

        project.untag(tag);

        return status(Http.Status.NO_CONTENT);
    }
}
