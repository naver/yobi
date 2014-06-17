/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2013 NAVER Corp.
 * http://yobi.io
 *
 * @Author Yi EungJun
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

import models.*;
import models.enumeration.Direction;
import models.enumeration.Operation;
import models.resource.Resource;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import play.data.Form;
import play.db.ebean.Model;
import play.i18n.Messages;
import play.mvc.Call;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import utils.*;

import java.io.IOException;

/**
 * The AbstractPostingApp class contains functions used in {@link BoardApp} and {@link IssueApp}
 */
public class AbstractPostingApp extends Controller {
    public static final int ITEMS_PER_PAGE = 15;

    /**
     * Search conditions.
     */
    public static class SearchCondition {
        public String orderBy;
        public String orderDir;
        public String filter;
        public int pageNum;

        /**
         * One of the basic search criteria that shows the first page, order by id desc.
         */
        public SearchCondition() {
            this.orderDir = Direction.DESC.direction();
            this.orderBy = "id";
            this.filter = "";
            this.pageNum = 1;
        }
    }

    /**
     * A handler that saves a new comment.
     *
     * Sets a current user as the author of a comment and 
     * saves the comment by taking the input values from {@code commentForm}.
     *
     * @param comment
     * @param commentForm
     * @param toView
     * @param containerUpdater
     * @return
     * @throws IOException
     */
    public static Result saveComment(final Comment comment, Form<? extends Comment> commentForm, final Call toView, Runnable containerUpdater) {
        if (commentForm.hasErrors()) {
            flash(Constants.WARNING, "post.comment.empty");
            return redirect(toView);
        }

        containerUpdater.run(); // this updates comment.issue or comment.posting;
        if(comment.id != null && AccessControl.isAllowed(UserApp.currentUser(), comment.asResource(), Operation.UPDATE)) {
            comment.update();
        } else {
            comment.setAuthor(UserApp.currentUser());
            comment.save();
        }


        // Attach all of the files in the current user's temporary storage.
        attachUploadFilesToPost(comment.asResource());

        String urlToView = RouteUtil.getUrl(comment);
        NotificationEvent.afterNewComment(comment);
        return redirect(urlToView);
    }


    /**
     * Deletes {@code target} and moves to {@code redirectTo}
     *
     * This method is used when deleting posts, issues or comments added to them.
     *
     * @param target
     * @param resource
     * @param redirectTo
     * @return
     */
    protected static Result delete(Model target, Resource resource, Call redirectTo) {
        if (!AccessControl.isAllowed(UserApp.currentUser(), resource, Operation.DELETE)) {
            return forbidden(ErrorViews.Forbidden.render("error.forbidden", resource.getProject()));
        }

        target.delete();

        // Returns 204 No Content and the Location header, if it is the XHR call.
        if(HttpUtil.isRequestedWithXHR(request())){
            response().setHeader("Location", redirectTo.url());
            return status(204);
        }

        return redirect(redirectTo);
    }

    /**
     * Fills out {@code posting} with {@code original} and updates.
     *
     * This is used when editing posts or issues.
     * If {@code noti} is not null when posts or issues are edited, this method sends notifications.
     *
     * @param original
     * @param posting
     * @param postingForm
     * @param redirectTo
     * @param preUpdateHook
     * @return
     */
    protected static Result editPosting(AbstractPosting original, AbstractPosting posting, Form<? extends AbstractPosting> postingForm, Call redirectTo, Runnable preUpdateHook) {
        if (postingForm.hasErrors()) {
            return badRequest(ErrorViews.BadRequest.render("error.validation", original.project));
        }

        if (!AccessControl.isAllowed(UserApp.currentUser(), original.asResource(), Operation.UPDATE)) {
            return forbidden(ErrorViews.Forbidden.render("error.forbidden", original.project));
        }

        if (posting.body == null) {
            return status(REQUEST_ENTITY_TOO_LARGE,
                    ErrorViews.RequestTextEntityTooLarge.render());
        }

        posting.id = original.id;
        posting.createdDate = original.createdDate;
        posting.updatedDate = JodaDateUtil.now();
        posting.authorId = original.authorId;
        posting.authorLoginId = original.authorLoginId;
        posting.authorName = original.authorName;
        posting.project = original.project;
        posting.setNumber(original.getNumber());
        preUpdateHook.run();
        posting.update();
        posting.updateProperties();

        // Attach the files in the current user's temporary storage.
        attachUploadFilesToPost(original.asResource());

        return redirect(redirectTo);
    }

    /**
     * Attaches temp files uploaded to a certain resource, such as a post or a comment, by a user.
     *
     * This method is used when linking upload files to a specified resource.
     * Each file id value separated by a comma is placed in 
     * {code AttachmentApp.TAG_NAME_FOR_TEMPORARY_UPLOAD_FILES}
     *
     * @param resource  issues, messages, comments
     */
    public static void attachUploadFilesToPost(Resource resource) {
        final String[] temporaryUploadFiles = getTemporaryFileListFromHiddenForm();
        if(isTemporaryFilesExist(temporaryUploadFiles)){
            int attachedFileCount = Attachment.moveOnlySelected(UserApp.currentUser().asResource(), resource,
                    temporaryUploadFiles);
            if( attachedFileCount != temporaryUploadFiles.length){
                flash(Constants.TITLE, Messages.get("post.popup.fileAttach.hasMissing", temporaryUploadFiles.length - attachedFileCount));
                flash(Constants.DESCRIPTION, Messages.get("post.popup.fileAttach.hasMissing.description", getTemporaryFilesServerKeepUpTimeOfMinuntes()));
            }
        }
    }

    private static long getTemporaryFilesServerKeepUpTimeOfMinuntes() {
        return AttachmentApp.TEMPORARYFILES_KEEPUP_TIME_MILLIS/(60*1000l);
    }

    private static String[] getTemporaryFileListFromHiddenForm() {
        Http.MultipartFormData body = request().body().asMultipartFormData();
        if (body == null) {
            return new String[] {};
        }
        String [] temporaryUploadFiles = body.asFormUrlEncoded().get(AttachmentApp.TAG_NAME_FOR_TEMPORARY_UPLOAD_FILES);
        if (temporaryUploadFiles == null) {
            return new String[] {};
        }
        final String CSV_DELEMETER = ",";
        return body.asFormUrlEncoded()
            .get(AttachmentApp.TAG_NAME_FOR_TEMPORARY_UPLOAD_FILES)[0].split(CSV_DELEMETER);
    }

    private static boolean isTemporaryFilesExist(String[] files) {
        if (ArrayUtils.getLength(files) == 0) {
            return false;
        }
        return StringUtils.isNotBlank(files[0]);
    }
}
