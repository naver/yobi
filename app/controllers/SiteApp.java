/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2012 NAVER Corp.
 * http://yobi.io
 *
 * @author Hwi Ahn
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

import com.avaje.ebean.Page;
import controllers.annotation.AnonymousCheck;
import info.schleichardt.play2.mailplugin.Mailer;
import models.*;
import models.enumeration.State;
import models.enumeration.UserState;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.eclipse.jgit.api.errors.GitAPIException;
import play.Configuration;
import play.Logger;
import play.db.ebean.Transactional;
import views.html.site.*;
import play.mvc.*;
import utils.*;

import java.util.*;

import static play.libs.Json.toJson;

/**
 * The Class SiteApp.
 */
 @With(SiteManagerAuthAction.class)
@AnonymousCheck
public class SiteApp extends Controller {

    private static final int PROJECT_COUNT_PER_PAGE = 25;
    private static final int POSTING_COUNT_PER_PAGE = 30;
    private static final int ISSUE_COUNT_PER_PAGE = 30;

    /**
     * Sends a mail
     * This method is used when a user sends a mail from the send mail page. 
     * It gets an email address, recipient, title, content and assign them to {@code email}.
     * The method sends the email and assign its result to {@code email}
     * And it sets an error message and checks whether it sent the email or not through {@code writeMail()}, and then moves to the send mail page.
     *
     * @return the result
     * @throws EmailException the email exception
     * @see {@link SiteApp#writeMail(String, boolean)}
     */
    public static Result sendMail() throws EmailException{
        SimpleEmail email = new SimpleEmail();

        Map<String, String[]> formData = request().body().asFormUrlEncoded();
        email.setFrom(utils.HttpUtil.getFirstValueFromQuery(formData, "from"));
        email.setSubject(utils.HttpUtil.getFirstValueFromQuery(formData, "subject"));
        email.addTo(utils.HttpUtil.getFirstValueFromQuery(formData, "to"));
        email.setMsg(utils.HttpUtil.getFirstValueFromQuery(formData, "body"));
        email.setCharset("utf-8");

        String errorMessage = null;
        boolean sended;
        String result = Mailer.send(email);
        Logger.info(">>>" + result);
        sended = true;
        return writeMail(errorMessage, sended);
    }

    /**
     * Moves to the send mail page.
     * This method is used when an administrator sends an email.
     * This method gets SMTP settings from {@code application.conf}.
     * It finds an item that hasn’t been set from {@code requiredItems} and saves it in {@code notConfiguredItems} and sends it to a page.
     * The mail sender consists of {@code smtp.user} and {@code smtp.domain}.
     *
     * @param errorMessage  mail sending error message
     * @param sended whether a mail has been sent or not
     * @return the result
     */
    public static Result writeMail(String errorMessage, boolean sended) {

        Configuration config = play.Play.application().configuration();
        List<String> notConfiguredItems = new ArrayList<>();
        String[] requiredItems = {"smtp.host", "smtp.user", "smtp.password"};
        for(String key : requiredItems) {
            if (config.getString(key) == null) {
                notConfiguredItems.add(key);
            }
        }

        String sender = utils.Config.getEmailFromSmtp();

        return ok(mail.render("title.sendMail", notConfiguredItems, sender, errorMessage, sended));
    }

    /**
     * Moves to the send mass mail page.
     * This method is used when a user sends mass mail.
     *
     * @return the result
     */
    public static Result massMail() {
        return ok(massMail.render("title.massMail"));
    }

    /**
     * Displays the list of all users.
     * This method is used when an administrator manages users.
     * It finds the list of members of the site by {@code loginId} and returns the list in {@link Page} format.
     * It refers its paging size to {@link User#USER_COUNT_PER_PAGE}
     *
     * @param pageNum pager number
     * @param loginId loginId
     * @return the result
     * @see {@link User#findUsers(int, String)}
     */
    public static Result userList(int pageNum, String query) {
        String state = StringUtils.defaultIfBlank(request().getQueryString("state"), UserState.ACTIVE.name());
        UserState userState = UserState.valueOf(state);
        Page<User> users = User.findUsers(pageNum -1, query, userState);
        return ok(userList.render("title.siteSetting", users, userState, query));
    }

    /**
     * Displays the list of entire posts.
     * This method is used in the administration page to manage posts.
     * It sorts the list of posts in descending order of date and gets the list corresponding to {@code pageNum}.
     *
     * @param pageNum page number
     * @return the result
     */
    public static Result postList(int pageNum) {
        Page<Posting> page = Posting.finder.order("createdDate DESC").findPagingList(POSTING_COUNT_PER_PAGE).getPage(pageNum - 1);
        return ok(postList.render("title.siteSetting", page));
    }

    /**
     * Displays the list of all issues grouped by state.
     * This method is used when managing issues in the administration page.
     * It sorts the list of issues in descending order of date and gets the list corresponding to {@code pageNum}.
     *
     * @param pageNum page number
     * @return the result
     */
    public static Result issueList(int pageNum) {
        String state = StringUtils.defaultIfBlank(request().getQueryString("state"), State.OPEN.name());
        State currentState = State.valueOf(state.toUpperCase());
        Page<Issue> page = Issue.findIssuesByState(ISSUE_COUNT_PER_PAGE, pageNum - 1, currentState);
        return ok(issueList.render("title.siteSetting", page, currentState));
    }

    /**
     * Deletes users
     * This method is used when a user needs to be deleted in the administration page.
     *
     * @param userId the user id
     * @return the result
     * @see {@link Project#isOnlyManager(Long)}
     */
    @Transactional
    public static Result deleteUser(Long userId) {
        if (User.findByLoginId(session().get("loginId")).isSiteManager()){
            if (Project.isOnlyManager(userId)) {
                flash(Constants.WARNING, "site.userList.deleteAlert");
                return forbidden();
            } else {
                User user = User.find.byId(userId);
                for (ProjectUser projectUser : user.projectUser) {
                    projectUser.delete();
                }
                user.changeState(UserState.DELETED);

                return redirect(routes.SiteApp.userList(1, null));
            }
        } else {
            flash(Constants.WARNING, "error.auth.unauthorized.waringMessage");
            return forbidden();
        }
    }

    /**
     * Gets the list of projects.
     * This method is used when setting projects in the administration page.
     * It gets the list of projects whose project name matches {@code project name}.
     *
     * @param projectName the project name
     * @param pageNum page number
     * @return the result
     * @see {@link Project#findByName(String, int, int)}
     */
    public static Result projectList(String projectName, int pageNum) {
        Page<Project> projects = Project.findByName(projectName, PROJECT_COUNT_PER_PAGE, pageNum);
        return ok(projectList.render("title.projectList", projects, projectName));
    }

    /**
     * Deletes projects.
     * This method is used when deleting projects at the project settings in the administration page.
     * It checks if the session {@code loginId} matches that of the site administrator and if it matches the administrator, it deletes the project. 
     * If the id isn’t belong to the administrator, it returns a warning message and redirects to the project settings. 
     *
     * @param projectId the project id
     * @return the result
     */
    @Transactional
    public static Result deleteProject(Long projectId){
        if( User.findByLoginId(session().get("loginId")).isSiteManager() ){
            Project.find.byId(projectId).delete();
        } else {
            flash(Constants.WARNING, "error.auth.unauthorized.waringMessage");
        }
        return redirect(routes.SiteApp.projectList(StringUtils.EMPTY, 0));
    }

    /**
     * Unlocks accounts
     * This method is used when unlocking the accounts of the administrator page.
     * If the {@code loginId} matches that of the site administrator and it isn’t {@code anonymous}, this method unlocks the account and redirects to the administrator page. 
     * If the session {@code loginId} matches the site administrator and {@code loginId} to be deleted is an anonymous user, it returns a warning message and redirects to the administrator page. 
     * If the session {@code loginId} is not the site administrator, it returns a warning message and redirects to the main page of Yobi.
     *
     * @param loginId the login id
     * @return the result
     */

    public static Result toggleAccountLock(String loginId, String state, String query){
        String stateParam = StringUtils.defaultIfBlank(state, UserState.ACTIVE.name());
        UserState userState = UserState.valueOf(stateParam);

        if(User.findByLoginId(session().get("loginId")).isSiteManager()){
            User targetUser = User.findByLoginId(loginId);
            if (targetUser.isAnonymous()){
                flash(Constants.WARNING, "user.notExists.name");
                return redirect(routes.SiteApp.userList(0, null));
            }
            if (targetUser.state == UserState.ACTIVE) {
                targetUser.changeState(UserState.LOCKED);
            } else {
                targetUser.changeState(UserState.ACTIVE);
            }
            return ok(userList.render("title.siteSetting", User.findUsers(0, query, userState), userState, query));
        }
        flash(Constants.WARNING, "error.auth.unauthorized.waringMessage");
        return redirect(routes.Application.index());
    }

    /**
     * Returns mass mail lists in JSON format.
     * This method is used when sending mass mails from the administrator page. 
     * If {@code currentUser} is not the site administrator, it returns a warning message and Forbidden response.
     * If the request content-type is not application/json, it returns {@link Http.Status#NOT_ACCEPTABLE}.
     * If {@code projects} is null, it returns an empty json object.
     * If it is the request to send to all, it returns the list of all users in json.
     * If the target is a member of a specific project, it returns the members of projects in json.
     *
     * @return the result
     */
    public static Result mailList() {
        Set<String> emails = new HashSet<>();
        Map<String, String[]> projects = request().body().asFormUrlEncoded();
        if(!UserApp.currentUser().isSiteManager()) {
            return forbidden(ErrorViews.Forbidden.render("error.auth.unauthorized.waringMessage"));
        }

        if (!request().accepts("application/json")) {
            return status(Http.Status.NOT_ACCEPTABLE);
        }

        if (projects == null) {
            return ok(toJson(new HashSet<String>()));
        }

        if (projects.containsKey("all")) {
            if (projects.get("all")[0].equals("true")) {
                for(User user : User.find.findList()) {
                    emails.add(user.email);
                }
            }
        } else {
            for(String[] projectNames : projects.values()) {
                String projectName = projectNames[0];
                String[] parts = projectName.split("/");
                String owner = parts[0];
                String name = parts[1];
                Project project = Project.findByOwnerAndProjectName(owner, name);
                for (ProjectUser projectUser : ProjectUser.findMemberListByProject(project.id)) {
                    Logger.debug(projectUser.user.email);
                    emails.add(projectUser.user.email);
                }
            }
        }

        return ok(toJson(emails));
    }

    /**
     * Hide the notification for Yobi updates.
     */
    public static Result unwatchUpdate() {
        YobiUpdate.isWatched = false;
        return ok();
    }

    /**
     * Show the page to update Yobi.
     */
    public static Result update() throws GitAPIException {
        String currentVersion = null;
        Exception exception = null;

        try {
            currentVersion = Config.getCurrentVersion();
            YobiUpdate.refreshVersionToUpdate();
        } catch (Exception e) {
            exception = e;
        }

        return ok(update.render("title.siteSetting", currentVersion,
                    YobiUpdate.versionToUpdate, exception));
    }

    /**
     * Diagnose Yobi
     * @return
     */
    public static Result diagnose() {
        return ok(diagnostic.render("title.siteSetting", Diagnostic.checkAll()));
    }
}
