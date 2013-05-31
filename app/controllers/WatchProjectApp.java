package controllers;

import models.Project;
import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import utils.Constants;

/**
 * @author Keesun Baik
 */
public class WatchProjectApp extends Controller {

    public static Result watch(String userName, String projectName) {
        Project project = Project.findByOwnerAndProjectName(userName, projectName);
        if(project == null) {
            return badProject(userName, projectName);
        }

        User user = UserApp.currentUser();
        if(user.isAnonymous()) {
            flash(Constants.WARNING, "user.login.alert");
            return redirect(routes.UserApp.loginForm());
        }

        user.addWatching(project);
        user.update();

        return redirect(routes.ProjectApp.project(userName, projectName));
    }

    public static Result unwatch(String userName, String projectName) {
        Project project = Project.findByOwnerAndProjectName(userName, projectName);
        if(project == null) {
            return badProject(userName, projectName);
        }

        User user = UserApp.currentUser();
        if(user.isAnonymous()) {
            flash(Constants.WARNING, "user.login.alert");
            return redirect(routes.UserApp.loginForm());
        }

        user.removeWatching(project);
        user.update();

        return redirect(routes.ProjectApp.project(userName, projectName));
    }

    private static Result badProject(String userName, String projectName) {
        return badRequest("No project matches given user name '" + userName + "' and project name '" + projectName + "'");
    }
}
