/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2012 NAVER Corp.
 * http://yobi.io
 *
 * @Author Ahn Hyeok Jun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package controllers;

import actions.DefaultProjectCheckAction;
import controllers.annotation.IsAllowed;
import models.Project;
import models.enumeration.Operation;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.codehaus.jackson.node.ObjectNode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.tmatesoft.svn.core.SVNException;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import playRepository.PlayRepository;
import playRepository.RepositoryService;
import utils.ErrorViews;
import utils.FileUtil;
import views.html.code.nohead;
import views.html.code.nohead_svn;
import views.html.code.view;

import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CodeApp extends Controller {
    public static String hostName;

    /**
     * Displays the default code browser.
     *
     * @param userName a project owner
     * @param projectName a project name
     */
    @IsAllowed(Operation.READ)
    public static Result codeBrowser(String userName, String projectName)
            throws IOException, UnsupportedOperationException, ServletException {
        Project project = Project.findByOwnerAndProjectName(userName, projectName);

        if (!RepositoryService.VCS_GIT.equals(project.vcs) && !RepositoryService.VCS_SUBVERSION.equals(project.vcs)) {
            return status(Http.Status.NOT_IMPLEMENTED, project.vcs + " is not supported!");
        }

        PlayRepository repository = RepositoryService.getRepository(project);

        // Displays the NOHEAD message if the repository is a Git repository that has no branch.
        if(repository.isEmpty()) {
            switch (project.vcs) {
                case RepositoryService.VCS_GIT:
                    return ok(nohead.render(project));
                case RepositoryService.VCS_SUBVERSION:
                    return ok(nohead_svn.render(project));
            }
        }

        String defaultBranch = project.defaultBranch();
        if (defaultBranch == null) {
            defaultBranch = "HEAD";
        } else if (defaultBranch.split("/").length >= 3) {
            defaultBranch = defaultBranch.split("/", 3)[2];
        }
        defaultBranch = URLEncoder.encode(defaultBranch, "UTF-8");
        return redirect(routes.CodeApp.codeBrowserWithBranch(userName, projectName, defaultBranch, ""));
    }

    /**
     * Displays a code browser that receives a branch and a file path as its parameters.
     *
     * @param userName a project owner
     * @param projectName a project name
     * @param branch a branch name
     * @param path a file path
     */
    @With(DefaultProjectCheckAction.class)
    public static Result codeBrowserWithBranch(String userName, String projectName, String branch, String path)
        throws UnsupportedOperationException, IOException, SVNException, GitAPIException, ServletException {
        Project project = Project.findByOwnerAndProjectName(userName, projectName);

        if (!RepositoryService.VCS_GIT.equals(project.vcs) && !RepositoryService.VCS_SUBVERSION.equals(project.vcs)) {
            return status(Http.Status.NOT_IMPLEMENTED, project.vcs + " is not supported!");
        }

        PlayRepository repository = RepositoryService.getRepository(project);
        ObjectNode fileInfo = repository.getMetaDataFromPath(branch, path);
        if (fileInfo == null) {
            return notFound(ErrorViews.NotFound.render("error.notfound"));
        }
        fileInfo.put("path", path);

        List<ObjectNode> recursiveData = new ArrayList<>();
        List<String> branches = repository.getBranches();

        /** If the given path is a folder and is not a top-level directory, it adds information to the path from the top level path in order **/
        if(fileInfo.get("type").getTextValue().equals("folder") && !path.equals("")){
            recursiveData.addAll(RepositoryService.getMetaDataFromAncestorDirectories(repository, branch, path));
        }
        recursiveData.add(fileInfo);

        return ok(view.render(project, branches, recursiveData, branch, path));
    }

    /**
     * This method is used to get the information about the project path specified by AJAX request.
     *
     * @param userName a project owner
     * @param projectName a project name
     * @param path the path of a file or folder
     */
    @With(DefaultProjectCheckAction.class)
    public static Result ajaxRequest(String userName, String projectName, String path) throws Exception{
        PlayRepository repository = RepositoryService.getRepository(userName, projectName);
        ObjectNode fileInfo = repository.getMetaDataFromPath(path);

        if(fileInfo != null) {
            return ok(fileInfo);
        } else {
            return notFound();
        }
    }

    /**
     * This method is used to get information about the specified path in the branch of a project called in AJAX request.
     *
     * @param userName a project owner
     * @param projectName a project name
     * @param branch a branch name
     * @param path the path of a file or folder
     */
    @With(DefaultProjectCheckAction.class)
    public static Result ajaxRequestWithBranch(String userName, String projectName, String branch, String path)
            throws UnsupportedOperationException, IOException, SVNException, GitAPIException, ServletException{
        CodeApp.hostName = request().host();
        PlayRepository repository = RepositoryService.getRepository(userName, projectName);
        ObjectNode fileInfo = repository.getMetaDataFromPath(branch, path);

        if(fileInfo != null) {
            return ok(fileInfo);
        } else {
            return notFound();
        }
    }

    /**
     * Displays the original file specified in a given project.
     *
     * @param userName
     * @param projectName
     * @param revision
     * @param path
     */
    @With(DefaultProjectCheckAction.class)
    public static Result showRawFile(String userName, String projectName, String revision, String path) throws Exception{
        byte[] fileAsRaw = RepositoryService.getFileAsRaw(userName, projectName, revision, path);
        if(fileAsRaw == null){
            return redirect(routes.CodeApp.codeBrowserWithBranch(userName, projectName, revision, path));
        }

        MediaType mediaType = FileUtil.detectMediaType(fileAsRaw, FilenameUtils.getName(path));

        String mediaTypeString = "text/plain";
        String charset = FileUtil.getCharset(mediaType);
        if (charset != null) {
            mediaTypeString += "; charset=" + charset;
        }

        return ok(fileAsRaw).as(mediaTypeString);
    }

    /**
     * 지정판 프로젝트의 지정한 이미지 파일 원본을 보여준다
     *
     * @param userName
     * @param projectName
     * @param path
     */
    @With(DefaultProjectCheckAction.class)
    public static Result showImageFile(String userName, String projectName, String revision, String path) throws Exception{
        final byte[] fileAsRaw = RepositoryService.getFileAsRaw(userName, projectName, revision, path);
        String mimeType = tika.detect(fileAsRaw);
        return ok(fileAsRaw).as(mimeType);
    }

    private static Tika tika = new Tika();

    /**
     * A function that returns the URL of a project repository
     * It is used to display the URL of a repository on the screen.
     *
     * @param ownerName
     * @param projectName
     */
    public static String getURL(String ownerName, String projectName) {
        Project project = Project.findByOwnerAndProjectName(ownerName, projectName);
        return getURL(project);
    }

    public static String getURL(Project project) {
        if (project == null) {
            return null;
        } else if (RepositoryService.VCS_GIT.equals(project.vcs)) {
            return utils.Url.create(Arrays.asList(project.owner, project.name));
        } else if (RepositoryService.VCS_SUBVERSION.equals(project.vcs)) {
            return utils.Url.create(Arrays.asList("svn", project.owner, project.name));
        } else {
            return null;
        }
    }

    /**
     * Return the URL of the project repository including user ID,
     * if there’s no current logged-in user information.
     * Example: protocol://user@host.name/path
     *
     * @param project
     */
    public static String getURLWithLoginId(Project project) {
        String url = getURL(project);
        if(url != null) {
            String loginId = session().get(UserApp.SESSION_LOGINID);
            if(loginId != null && !loginId.isEmpty()) {
                url = url.replace("://", "://" + loginId + "@");
            }
        }
        return url;
    }

    /**
     * Opens the selected file of a specified project.
     *
     * @param userName
     * @param revision
     * @param path
     */
    @IsAllowed(Operation.READ)
    public static Result openFile(String userName, String projectName, String revision,
                           String path) throws Exception{
        byte[] raw = RepositoryService.getFileAsRaw(userName, projectName, revision, path);

        if(raw == null){
            return notFound(ErrorViews.NotFound.render("error.notfound"));
        }

        return ok(raw).as(FileUtil.detectMediaType(raw, FilenameUtils.getName(path)).toString());
    }
}
