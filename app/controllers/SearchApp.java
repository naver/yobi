/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2014 NAVER Corp.
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
package controllers;

import controllers.annotation.AnonymousCheck;
import controllers.annotation.IsAllowed;
import models.*;
import models.enumeration.Operation;
import models.enumeration.SearchType;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Controller;
import play.mvc.Result;
import utils.ErrorViews;
import views.html.search.result;

@AnonymousCheck
public class SearchApp extends Controller {

    private static final PageParam DEFAULT_PAGE = new PageParam(0, 20);

    /**
     * Search contents that current user can read in all projects.
     *
     * @return
     */
    public static Result searchInAll() {
        // SearchCondition from param
        String searchTypeValue = request().getQueryString("searchType");
        String keyword = request().getQueryString("keyword");
        PageParam pageParam = getPage();

        if(StringUtils.isEmpty(keyword) || StringUtils.isEmpty(searchTypeValue)) {
            return badRequest(ErrorViews.BadRequest.render());
        }

        User user = UserApp.currentUser();
        SearchType searchType = SearchType.getValue(searchTypeValue);

        if(searchType == SearchType.NA) {
            return badRequest(ErrorViews.BadRequest.render());
        }

        return ok(result.render("title.search", null, null, Search.searchInAll(keyword, user, searchType, pageParam)));
    }

    /**
     * Search contents that current user can read in a group.
     *
     * @param organizationName
     * @return
     */
    public static Result searchInAGroup(String organizationName) {
        String searchTypeValue = request().getQueryString("searchType");
        String keyword = request().getQueryString("keyword");
        PageParam pageParam = getPage();

        if(StringUtils.isEmpty(organizationName)
                || StringUtils.isEmpty(keyword)
                || StringUtils.isEmpty(searchTypeValue)) {
            return badRequest();
        }

        Organization organization = Organization.findByName(organizationName);
        User user = UserApp.currentUser();
        SearchType searchType = SearchType.getValue(searchTypeValue);

        if(searchType == SearchType.NA || organization == null) {
            return badRequest(ErrorViews.BadRequest.render());
        }

        return ok(result.render("title.search", organization, null,
                Search.searchInGroup(keyword, user, organization, searchType, pageParam)));
    }

    /**
     * Search contents that current user can read in a project.
     *
     * @param loginId
     * @param projectName
     * @return
     */
    @IsAllowed(Operation.READ)
    public static Result searchInAProject(String loginId, String projectName) {
        String searchTypeValue = request().getQueryString("searchType");
        String keyword = request().getQueryString("keyword");
        Project project = Project.findByOwnerAndProjectName(loginId, projectName);
        PageParam pageParam = getPage();

        if(StringUtils.isEmpty(keyword)
                || StringUtils.isEmpty(searchTypeValue)
                || project == null) {
            return badRequest(ErrorViews.BadRequest.render());
        }

        User user = UserApp.currentUser();
        SearchType searchType = SearchType.getValue(searchTypeValue);

        if(searchType == SearchType.NA || searchType == SearchType.PROJECT) {
            return badRequest(ErrorViews.BadRequest.render());
        }

        return ok(result.render("title.search", null, project,
                Search.searchInProject(keyword, user, project, searchType, pageParam)));
    }

    private static PageParam getPage() {
        PageParam pageParam = new PageParam(DEFAULT_PAGE.getPage(), DEFAULT_PAGE.getSize());
        String pageNumString = request().getQueryString("pageNum");
        if(pageNumString != null) {
            int pageNum = Integer.parseInt(pageNumString);
            pageParam.setPage(pageNum - 1);
        }
        return pageParam;
    }

}
