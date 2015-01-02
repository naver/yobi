/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2013 NAVER Corp.
 * http://yobi.io
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

package security;

import controllers.UserApp;
import models.User;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.mvc.Http;

public class BasicRememberMeService implements RememberMeService, LogoutHandler {
    @Override
    public User autoLogin(Http.Context context) {
        Http.Cookie cookie = context.request().cookies().get(UserApp.TOKEN);
        if (cookie == null) {
            return User.anonymous;
        }

        String[] subject = StringUtils.split(cookie.value(), UserApp.TOKEN_SEPARATOR);
        if (ArrayUtils.getLength(subject) != UserApp.TOKEN_LENGTH) {
            cancelCookie(context);
            return User.anonymous;
        }

        User user = UserApp.authenticateWithHashedPassword(subject[0], subject[1]);

        if (user.isAnonymous()) {
            cancelCookie(context);
            return User.anonymous;
        }

        return user;
    }

    @Override
    public void loginSuccess(Http.Context context, User user) {
        context.response().setCookie(UserApp.TOKEN, user.loginId + ":" + user.password, UserApp.MAX_AGE);
        Logger.debug("remember me enabled");
    }

    @Override
    public void logout(Http.Context context, User user) {
        Logger.debug("Logout of user " + (user == null ? "Unknown" : user.name));
        cancelCookie(context);
    }

    protected void cancelCookie(Http.Context context) {
        Logger.debug("cancelling cookie");
        context.response().discardCookie(UserApp.TOKEN);
    }
}
