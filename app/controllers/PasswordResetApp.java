/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2013 NAVER Corp.
 * http://yobi.io
 *
 * @Author Suwon Chae
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

import info.schleichardt.play2.mailplugin.Mailer;
import utils.PasswordReset;
import models.User;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import play.Configuration;
import play.Logger;
import play.data.DynamicForm;
import play.mvc.*;
import play.i18n.Messages;
import utils.Constants;
import views.html.user.login;
import views.html.user.resetPassword;
import views.html.site.lostPassword;

import static play.data.Form.form;

public class PasswordResetApp extends Controller {

    /**
     * Moves to the password reset mail sending page.
     * This method is used when a user clicks the “forgot password” link.
     *
     * @return the password reset mail sending page.
     */
    public static Result lostPassword(){
        // render(message: String, sender: String, errorMessage: String, isSent: Boolean)
        return ok(lostPassword.render("site.resetPasswordEmail.title", null, null, false));
    }

    /**
     * Requests to send the password reset mail.
     * This method is used when a user typed in his/her log-in id and email address to request to reset a password.
     * - Gets {@code loginId}and {@code emailAddress} from the form
     * - Sends out a mail with the password reset link if the requested {@code loginId} exists and matches {@code email}
     * - If not, return error message
     *
     * @return request for password reset mail      
     */
    public static Result requestResetPasswordEmail(){
        DynamicForm requestData = form().bindFromRequest();
        String loginId = requestData.get("loginId");
        String emailAddress = requestData.get("emailAddress");

        Logger.debug("request reset password email by [" + loginId + ":" + emailAddress + "]");

        User targetUser = User.findByLoginId(loginId);

        boolean isMailSent = false;
        String errorMessage = null;
        if(!targetUser.isAnonymous() && targetUser.email.equals(emailAddress)) {
           String hashString = PasswordReset.generateResetHash(targetUser.loginId);
           PasswordReset.addHashToResetTable(targetUser.loginId, hashString);
           isMailSent = sendPasswordResetMail(targetUser, hashString);
        } else {
            Logger.debug("wrong user: " + loginId);
            errorMessage = Messages.get("site.resetPasswordEmail.invalidRequest");
        }
        return ok(lostPassword.render("site.resetPasswordEmail.title", emailAddress, errorMessage, isMailSent));
    }

    /**
     * Sends out an email that attached a link to the password reset page.
     * This method is used when it needs to send a password reset email to {@code hashString} generated to reset password.
     * - This method gets {@code smtp.user} and {@code smtp.domain} from play’s {@code application.conf} and uses it as a sender’s address.
     * - It attaches the password reset URL to the mail content and sends it.
     *
     * @param user  user information found through the email request.
     * @param hashString  randomized hash string for the given user
     * @return
     */
    private static boolean sendPasswordResetMail(User user, String hashString) {
        // SiteApp.sendMail() should be integrated into this method.
        // The section for checking site email settings is missing.
        Configuration config = play.Play.application().configuration();
        String sender = config.getString("smtp.user") + "@" + config.getString("smtp.domain");
        String resetPasswordUrl = getResetPasswordUrl(hashString);

        try {
            SimpleEmail email = new SimpleEmail();
            email.setFrom(sender)
                 .setSubject("[" + utils.Config.getSiteName() + "] " + Messages.get("site.resetPasswordEmail.title"))
                 .addTo(user.email)
                 .setMsg(Messages.get("site.resetPasswordEmail.mailContents") + "\n\n" + resetPasswordUrl)
                 .setCharset("utf-8");

            Logger.debug("password reset mail send: " +Mailer.send(email));
            return true;
        } catch (EmailException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Generates URL link for password reset
     * - Reads {@code application.hostname} settings and {@code application.port} settings from application.conf.
     * - If neither has been set up, you should assume that this is under development environment and generate a URL link with LOCAL_HOST_IP:DEV_MODE_PORT
     *
     * @param hashString
     * @return a URL link for password rest
     */
    private static String getResetPasswordUrl(String hashString) {
        Configuration config = play.Play.application().configuration();
        String hostname = config.getString("application.hostname");
        if(hostname == null) hostname = request().host();

        return "http://" + hostname + "/resetPassword?s=" + hashString;
    }

    /**
     * Moves to the password reset page
     */
    public static Result resetPasswordForm(String hashString){
        return ok(resetPassword.render("title.resetPassword", form(User.class), hashString));
    }

    /**
     * Changes the password of a user who requested to send a password reset email into a new password
     * - Gets {@code hashString}and {@code password} from the requested form to verify.
     * - Checks if {@code hashString} is valid.
     * - If it is valid, this method resets password; otherwise, it writes log and moves to the log-in page.
     *
     * @return log-in page
     */
    public static Result resetPassword(){
        DynamicForm requestData = form().bindFromRequest();
        String hashString = requestData.get("hashString");
        String newPassword = requestData.get("password");

        if(PasswordReset.isValidResetHash(hashString)){
            PasswordReset.resetPassword(hashString, newPassword);
            Logger.debug("Password was reset");
        } else {
            Logger.debug("Not a valid request!");
        }
        flash(Constants.WARNING, "user.loginWithNewPassword");
        return ok(login.render("title.login", form(User.class), null));
    }
}
