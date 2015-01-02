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
import org.apache.commons.lang3.StringUtils;
import play.mvc.Http;

public class BasicSecurityContextRepository implements SecurityContextRepository, LogoutHandler {
    @Override
    public User loadUser(Http.Context context) {
        User user = getUserFromSession(context);
        if (!user.isAnonymous()) {
            return user;
        }
        return getUserFromContext(context);
    }

    @Override
    public void saveUser(User user, Http.Context context) {
        context.args.put(UserApp.TOKEN_USER, user);

        if (!user.isAnonymous() && getUserFromSession(context).isAnonymous()) {
            context.session().put(UserApp.SESSION_USERID, String.valueOf(user.id));
            context.session().put(UserApp.SESSION_LOGINID, user.loginId);
            context.session().put(UserApp.SESSION_USERNAME, user.name);
        }
    }

    private User getUserFromSession(Http.Context context) {
        String userId = context.session().get(UserApp.SESSION_USERID);
        if (userId == null) {
            return User.anonymous;
        }
        if (!StringUtils.isNumeric(userId)) {
            context.session().clear();
            return User.anonymous;
        }
        User user = User.find.byId(Long.valueOf(userId));
        if (user == null) {
            context.session().clear();
            return User.anonymous;
        }
        return user;
    }

    private User getUserFromContext(Http.Context context) {
        Object cached = context.args.get(UserApp.TOKEN_USER);

        if (cached instanceof User) {
            return (User) cached;
        }

        return User.anonymous;
    }

    @Override
    public void logout(Http.Context context, User user) {
        context.session().clear();
    }
}
