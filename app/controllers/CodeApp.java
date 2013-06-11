package controllers;

import models.Project;
import models.User;
import models.enumeration.Operation;
import org.apache.tika.Tika;
import org.codehaus.jackson.node.ObjectNode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.tmatesoft.svn.core.SVNException;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import playRepository.RepositoryService;
import utils.AccessControl;
import views.html.code.view;
import views.html.code.nohead;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CodeApp extends Controller {
	public static Result codeBrowser(String userName, String projectName)
            throws IOException, UnsupportedOperationException, ServletException {
        Project project = ProjectApp.getProject(userName, projectName);
        if (project == null) {
            return notFound();
        }

        if (!AccessControl.isAllowed(UserApp.currentUser(), project.asResource(), Operation.READ)) {
            return unauthorized(views.html.error.unauthorized.render(project));
        }

        if (!RepositoryService.VCS_GIT.equals(project.vcs) && !RepositoryService.VCS_SUBVERSION.equals(project.vcs)) {
            return status(Http.Status.NOT_IMPLEMENTED, project.vcs + " is not supported!");
        }

        List<String> branches = RepositoryService.getRepository(project).getBranches();

        if (RepositoryService.VCS_GIT.equals(project.vcs) && branches.size() == 0) {
            return ok(nohead.render(project));
        }

        return ok(view.render(project, branches));
    }
	public static Result codeBrowserWithBranch(String userName, String projectName, String branch)
            throws IOException, UnsupportedOperationException, ServletException {
        Project project = ProjectApp.getProject(userName, projectName);

        if (!RepositoryService.VCS_GIT.equals(project.vcs) && !RepositoryService.VCS_SUBVERSION.equals(project.vcs)) {
            return status(Http.Status.NOT_IMPLEMENTED, project.vcs + " is not supported!");
        }

        List<String> branches = RepositoryService.getRepository(project).getBranches();

        return ok(view.render(project, branches));
    }

    public static Result ajaxRequest(String userName, String projectName, String path) throws Exception{
        ObjectNode findFileInfo = RepositoryService.getMetaDataFrom(userName, projectName, path);
        if(findFileInfo != null) {
            return ok(findFileInfo);
        } else {
            return notFound();
        }
    }
    public static Result ajaxRequestWithBranch(String userName, String projectName, String branch, String path)
            throws UnsupportedOperationException, IOException, SVNException, GitAPIException, ServletException{
        ObjectNode findFileInfo = RepositoryService.getMetaDataFrom(userName, projectName, path, branch);
        if(findFileInfo != null) {
            return ok(findFileInfo);
        } else {
            return notFound();
        }
    }

    public static Result showRawFile(String userName, String projectName, String path) throws Exception{
        return ok(RepositoryService.getFileAsRaw(userName, projectName, path));
    }

    public static Result showImageFile(String userName, String projectName, String path) throws Exception{
        final byte[] fileAsRaw = RepositoryService.getFileAsRaw(userName, projectName, path);
        String mimeType = tika.detect(fileAsRaw);
        return ok(fileAsRaw).as(mimeType);
    }

    private static Tika tika = new Tika();

    public static String getURL(String ownerName, String projectName) {
        Project project = ProjectApp.getProject(ownerName, projectName);
        return getURL(project);
    }

    public static String getURL(Project project) {
        if (project == null) {
            return null;
        } else if (RepositoryService.VCS_GIT.equals(project.vcs)) {
            return utils.Url.create(Arrays.asList(project.owner, project.name), request().host());
        } else if (RepositoryService.VCS_SUBVERSION.equals(project.vcs)) {
            return utils.Url.create(Arrays.asList("svn", project.owner, project.name), request().host());
        } else {
            return null;
        }
    }

    public static String getURLWithLoginId(Project project) {
        String url = getURL(project);
        if(url != null && project.vcs.equals(RepositoryService.VCS_GIT)) {
            String loginId = session().get(UserApp.SESSION_LOGINID);
            if(loginId != null && !loginId.isEmpty()) {
                url = url.replace("://", "://" + loginId + "@");
            }
        }
        return url;
    }
}
