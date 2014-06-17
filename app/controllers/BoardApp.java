/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2012 NAVER Corp.
 * http://yobi.io
 *
 * @Author Sangcheol Hwang
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

import actions.AnonymousCheckAction;
import actions.DefaultProjectCheckAction;
import actions.NullProjectCheckAction;

import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.Page;

import controllers.annotation.IsAllowed;
import controllers.annotation.IsCreatable;
import models.*;
import models.enumeration.Operation;
import models.enumeration.ResourceType;

import org.codehaus.jackson.node.ObjectNode;

import play.data.Form;
import play.db.ebean.Transactional;
import play.libs.Json;
import play.mvc.Call;
import play.mvc.Result;
import play.mvc.With;
import utils.AccessControl;
import utils.ErrorViews;
import utils.JodaDateUtil;
import views.html.board.create;
import views.html.board.edit;
import views.html.board.list;
import views.html.board.view;

import java.io.IOException;
import java.util.List;

import static com.avaje.ebean.Expr.icontains;

public class BoardApp extends AbstractPostingApp {
    public static class SearchCondition extends AbstractPostingApp.SearchCondition {
        private ExpressionList<Posting> asExpressionList(Project project) {
            ExpressionList<Posting> el = Posting.finder.where().eq("project.id", project.id);

            if (filter != null) {
                el.or(icontains("title", filter), icontains("body", filter));
            }

            if (orderBy != null) {
                el.orderBy(orderBy + " " + orderDir);
            }

            return el;
        }
    }

    /**
     * Searches the list of posts.
     *
     * This method is used when searching/browsing a certain project’s list of posts.
     * It checks if a user has access right.
     * If the user has no access right, then it returns forbidden access.
     * It displays notices and the list of posts that match a search condition.
     *
     * @param userName a project owner
     * @param projectName a project name
     * @param pageNum a page number
     * @return
     */
    @IsAllowed(value = Operation.READ, resourceType = ResourceType.PROJECT)
    public static Result posts(String userName, String projectName, int pageNum) {
        Project project = Project.findByOwnerAndProjectName(userName, projectName);

        Form<SearchCondition> postParamForm = new Form<>(SearchCondition.class);
        SearchCondition searchCondition = postParamForm.bindFromRequest().get();
        searchCondition.pageNum = pageNum - 1;
        if(searchCondition.orderBy.equals("id")) {
            searchCondition.orderBy = "createdDate";
        }

        ExpressionList<Posting> el = searchCondition.asExpressionList(project);
        el.eq("notice", false);
        Page<Posting> posts = el.findPagingList(ITEMS_PER_PAGE).getPage(searchCondition.pageNum);
        List<Posting> notices = Posting.findNotices(project);

        return ok(list.render("menu.board", project, posts, searchCondition, notices));
    }

    /**
     * Displays a form for a new post.
     *
     * This method is used when adding a new post.
     * If the user has permission to write a notice, it activates a checkbox to be selected
     * when the post is to be a notice.
     *
     * @param userName a project owner
     * @param projectName a project name
     * @return
     */
    @With(AnonymousCheckAction.class)
    @IsCreatable(ResourceType.BOARD_POST)
    public static Result newPostForm(String userName, String projectName) {
        Project project = Project.findByOwnerAndProjectName(userName, projectName);

        boolean isAllowedToNotice =
                AccessControl.isProjectResourceCreatable(UserApp.currentUser(), project, ResourceType.BOARD_NOTICE);

        return ok(create.render("post.new", new Form<>(Posting.class), project, isAllowedToNotice));
    }

    /**
     * Adds a post.
     *
     * This method is called when writing and saving a post.
     * It checks if a user has permission to write. If the user has no access right,
     * then it returns forbidden access.
     *
     * @param userName a project owner
     * @param projectName a project name
     * @return
     */
    @Transactional
    @IsCreatable(ResourceType.BOARD_POST)
    public static Result newPost(String userName, String projectName) {
        Form<Posting> postForm = new Form<>(Posting.class).bindFromRequest();
        Project project = Project.findByOwnerAndProjectName(userName, projectName);

        if (postForm.hasErrors()) {
            boolean isAllowedToNotice =
                    AccessControl.isProjectResourceCreatable(UserApp.currentUser(), project, ResourceType.BOARD_NOTICE);
            return badRequest(create.render("error.validation", postForm, project, isAllowedToNotice));
        }

        final Posting post = postForm.get();

        if (post.body == null) {
            return status(REQUEST_ENTITY_TOO_LARGE, ErrorViews.RequestTextEntityTooLarge.render());
        }

        post.createdDate = JodaDateUtil.now();
        post.updatedDate = JodaDateUtil.now();
        post.setAuthor(UserApp.currentUser());
        post.project = project;

        post.save();

        attachUploadFilesToPost(post.asResource());

        NotificationEvent.afterNewPost(post);

        return redirect(routes.BoardApp.post(project.owner, project.name, post.getNumber()));
    }

    /**
     * Searches posts.
     *
     * This method is used when a user used the advanced search.
     * It checks if a user has permission to access. If the user has no access right, 
     * then it returns forbidden access. 
     * If there’s no content that matches the posting ID, return a message: No posts have been found.
     * If json is included in the ACCEPT header, the method sends the content of POST to JSON.
     *
     * @param userName a project owner
     * @param projectName project name
     * @param number number a post number
     * @return
     */
    @IsAllowed(value = Operation.READ, resourceType = ResourceType.BOARD_POST)
    public static Result post(String userName, String projectName, Long number) {
        Project project = Project.findByOwnerAndProjectName(userName, projectName);
        Posting post = Posting.findByNumber(project, number);

        if(request().getHeader("Accept").contains("application/json")) {
            ObjectNode json = Json.newObject();
            json.put("title", post.title);
            json.put("body", post.body);
            json.put("author", post.authorLoginId);
            return ok(json);
        }

        UserApp.currentUser().visits(project);
        Form<PostingComment> commentForm = new Form<>(PostingComment.class);
        return ok(view.render(post, commentForm, project));
    }

    /**
     * Displays an editing form.
     *
     * This method is used when a user tries to edit a post.
     * It checks if a user has permission to edit. If the user has no access right,
     * then it returns forbidden access.
     * It activates a checkbox to be selected if the user has permission to write a notice.
     * 
     * @param owner a project owner
     * @param projectName a project number
     * @param number number a post number
     * @return
     */
    @With(NullProjectCheckAction.class)
    public static Result editPostForm(String owner, String projectName, Long number) {
        Project project = Project.findByOwnerAndProjectName(owner, projectName);
        Posting posting = Posting.findByNumber(project, number);

        if (!AccessControl.isAllowed(UserApp.currentUser(), posting.asResource(), Operation.UPDATE)) {
            return forbidden(ErrorViews.Forbidden.render("error.forbidden", project));
        }

        Form<Posting> editForm = new Form<>(Posting.class).fill(posting);
        boolean isAllowedToNotice = ProjectUser.isAllowedToNotice(UserApp.currentUser(), project);

        return ok(edit.render("post.modify", editForm, posting, number, project, isAllowedToNotice));
    }

    /**
     * Edits and saves a post.
     *
     * This method is used when a user finished editing and tries to save.
     * It saves the change and returns to the first page of the list.
     *
     * @param userName a project owner
     * @param projectName a project name
     * @param number number a post number
     * @return
     * @see AbstractPostingApp#editPosting(models.AbstractPosting, models.AbstractPosting, play.data.Form
     */
    @Transactional
    @With(NullProjectCheckAction.class)
    public static Result editPost(String userName, String projectName, Long number) {
        Form<Posting> postForm = new Form<>(Posting.class).bindFromRequest();
        Project project = Project.findByOwnerAndProjectName(userName, projectName);

        if (postForm.hasErrors()) {
            boolean isAllowedToNotice =
                    AccessControl.isProjectResourceCreatable(UserApp.currentUser(), project, ResourceType.BOARD_NOTICE);
            return badRequest(edit.render("error.validation", postForm, Posting.findByNumber(project, number), number, project, isAllowedToNotice));
        }

        final Posting post = postForm.get();
        final Posting original = Posting.findByNumber(project, number);

        Call redirectTo = routes.BoardApp.post(project.owner, project.name, number);
        Runnable updatePostingBeforeUpdate = new Runnable() {
            @Override
            public void run() {
                post.comments = original.comments;
            }
        };

        return editPosting(original, post, postForm, redirectTo, updatePostingBeforeUpdate);
    }

    /**
     * Deletes a post.
     *
     * This method is used when a user tries to delete a post.
     * It deletes a post and returns to the first page of the list.
     *
     * @param owner a project owner
     * @param projectName a project name
     * @param number number a post number
     * @return
     * @see controllers.AbstractPostingApp#delete(play.db.ebean.Model, models.resource.Resource, play.mvc.Call)
     */
    @Transactional
    @IsAllowed(value = Operation.DELETE, resourceType = ResourceType.BOARD_POST)
    public static Result deletePost(String owner, String projectName, Long number) {
        Project project = Project.findByOwnerAndProjectName(owner, projectName);
        Posting posting = Posting.findByNumber(project, number);
        Call redirectTo = routes.BoardApp.posts(project.owner, project.name, 1);

        return delete(posting, posting.asResource(), redirectTo);
    }

    /**
     * Adds a comment
     *
     * This method is used when a user wrote a comment and tries to save the comment.
     * It carries out a validation check and if there’s an error, the method returns bad request
     * It saves the user’s comment and returns to the detailed post page. 
     *
     * @param owner a project owner
     * @param projectName a project name
     * @param number number a post number
     * @return
     * @throws IOException
     * @see controllers.AbstractPostingApp#saveComment(models.Comment, play.data.Form, play.mvc.Call, Runnable)
     */
    @Transactional
    @IsAllowed(value = Operation.READ, resourceType = ResourceType.BOARD_POST)
    @With(NullProjectCheckAction.class)
    public static Result newComment(String owner, String projectName, Long number) throws IOException {
        Project project = Project.findByOwnerAndProjectName(owner, projectName);
        final Posting posting = Posting.findByNumber(project, number);
        Call redirectTo = routes.BoardApp.post(project.owner, project.name, number);
        Form<PostingComment> commentForm = new Form<>(PostingComment.class)
                .bindFromRequest();

        if (commentForm.hasErrors()) {
            return badRequest(views.html.error.badrequest.render("error.validation", project));
        }

        if (!AccessControl.isResourceCreatable(
                    UserApp.currentUser(), posting.asResource(), ResourceType.NONISSUE_COMMENT)) {
            return forbidden(ErrorViews.Forbidden.render("error.forbidden", project));
        }

        final PostingComment comment = commentForm.get();
        PostingComment existingComment = PostingComment.find.where().eq("id", comment.id).findUnique();
        if( existingComment != null){
            existingComment.contents = comment.contents;
            return saveComment(existingComment, commentForm, redirectTo, getContainerUpdater(posting, comment));
        } else {
            return saveComment(comment, commentForm, redirectTo, getContainerUpdater(posting, comment));
        }
    }

    private static Runnable getContainerUpdater(final Posting posting, final PostingComment comment) {
        return new Runnable() {
            @Override
            public void run() {
                comment.posting = posting;
            }
        };
    }
    /**
     * Deletes a comment.
     *
     * This method is used when a user tries to delete a comment.
     * It deletes a comment and returns to the detailed post.
     *
     * @param userName a project owner
     * @param projectName a project name
     * @param number number a post number
     * @param commentId a comment ID
     * @return
     * @see controllers.AbstractPostingApp#delete(play.db.ebean.Model, models.resource.Resource, play.mvc.Call)
     */
    @Transactional
    @With(NullProjectCheckAction.class)
    public static Result deleteComment(String userName, String projectName, Long number, Long commentId) {
        Comment comment = PostingComment.find.byId(commentId);
        Project project = comment.asResource().getProject();
        Call redirectTo = routes.BoardApp.post(project.owner, project.name, number);

        return delete(comment, comment.asResource(), redirectTo);
    }
}
