/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2013 NAVER Corp.
 * http://yobi.io
 *
 * @author Wansoon Park
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
package utils;

import controllers.UserApp;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Result;
import play.libs.F.Promise;

/**
 * The Class SiteManagerAuthAction.
 */
public class SiteManagerAuthAction extends Action.Simple {
    @Override
    public Promise<Result> call(Context context) throws Throwable {
        if (!UserApp.currentUser().isSiteManager()) {
            return Promise.pure((Result) forbidden(ErrorViews.Forbidden.render("error.auth.unauthorized.waringMessage")));
        }
        return delegate.call(context);
    }
}
