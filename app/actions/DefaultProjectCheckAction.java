/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2013 NAVER Corp.
 * http://yobi.io
 *
 * @author Keesun Baik
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
package actions;

import actions.support.PathParser;
import models.Project;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Result;
import play.libs.F.Promise;

/**
 * Default implementation of {@link AbstractProjectCheckAction}.
 * Just execute what {@link AbstractProjectCheckAction} provides.
 *
 * @see {@link AbstractProjectCheckAction}
 * @author Keesun Baik
 */
public class DefaultProjectCheckAction extends AbstractProjectCheckAction<Void> {
    @Override
    protected Promise<Result> call(Project project, Context context, PathParser parser) throws Throwable {
        return this.delegate.call(context);
    }
}
