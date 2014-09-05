/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2012 NAVER Corp.
 * http://yobi.io
 *
 * @Author Tae
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
import controllers.annotation.IsAllowed;
import controllers.annotation.IsCreatable;
import models.Attachment;
import models.Milestone;
import models.Project;
import models.enumeration.Direction;
import models.enumeration.Operation;
import models.enumeration.ResourceType;
import models.enumeration.State;
import play.data.Form;
import play.db.ebean.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import utils.Constants;
import utils.ErrorViews;
import utils.HttpUtil;
import utils.JodaDateUtil;
import views.html.milestone.create;
import views.html.milestone.edit;
import views.html.milestone.list;
import views.html.milestone.view;

import java.util.List;

import static play.data.Form.form;

/**
 * Managing milestones
 */
public class MilestoneApp extends Controller {

    public static class MilestoneCondition {
        public String state    = "open";
        public String orderBy  = "dueDate";
        public String orderDir = "asc";

        public MilestoneCondition() {
            this.state    = "open";
            this.orderBy  = "dueDate";
            this.orderDir = "asc";
        }
    }

    /**
     * Retrieves the milestones list of the project that matches {@code userName} and {@code projectName}
     * when: GET /:user/:project/milestones
     *
     * Sets the basic search and sorting options with the input values from {@link MilestoneCondition}
     *
     * @param userName
     * @param projectName
     * @return
     */
    @IsAllowed(Operation.READ)
    public static Result milestones(String userName, String projectName) {
        Project project = Project.findByOwnerAndProjectName(userName, projectName);
        MilestoneCondition mCondition = form(MilestoneCondition.class).bindFromRequest().get();

        List<Milestone> milestones = Milestone.findMilestones(project.id,
                State.getValue(mCondition.state),
                mCondition.orderBy,
                Direction.getValue(mCondition.orderDir));

        return ok(list.render("title.milestoneList", milestones, project, mCondition));
    }

    /**
     * Moves to an input form to add a new milestone of a project that matches {@code userName} and {@code projectName}
     * when: GET /:user/:project/newMilestoneForm
     *
     * If no project matches the given condition, this method returns {@link #notFound()}
     *
     * @param userName
     * @param projectName
     * @return
     */
    @With(AnonymousCheckAction.class)
    @IsCreatable(ResourceType.MILESTONE)
    public static Result newMilestoneForm(String userName, String projectName) {
        Project project = Project.findByOwnerAndProjectName(userName, projectName);
        return ok(create.render("title.newMilestone", new Form<>(Milestone.class), project));
    }

    /**
     * Adds a new milestone to a project that matches {@code userName} and {@code *projectName}
     * when: POST /:user/:project/milestones
     *
     * Creates a new milestone by using the input data from {@link Milestone}
     * If there’s no project matching the given condition, the method returns {@link #notFound()}
     * Checks if there is any milestone that has the same with it.
     * When the method found a milestone with the same name, you will move to the new milestone form.
     *
     * @param userName
     * @param projectName
     * @return
     * @see {@link #validate(models.Project, play.data.Form)}
     */
    @Transactional
    @IsCreatable(ResourceType.MILESTONE)
    public static Result newMilestone(String userName, String projectName) {
        Form<Milestone> milestoneForm = new Form<>(Milestone.class).bindFromRequest();
        Project project = Project.findByOwnerAndProjectName(userName, projectName);

        validate(project, milestoneForm);
        if (milestoneForm.hasErrors()) {
            return ok(create.render("title.newMilestone", milestoneForm, project));
        } else {
            Milestone newMilestone = milestoneForm.get();

            if (newMilestone.contents == null) {
                return status(REQUEST_ENTITY_TOO_LARGE,
                        ErrorViews.RequestTextEntityTooLarge.render());
            }

            newMilestone.project = project;
            newMilestone.dueDate = JodaDateUtil.lastSecondOfDay(newMilestone.dueDate);
            Milestone.create(newMilestone);
            Attachment.moveAll(UserApp.currentUser().asResource(), newMilestone.asResource());
            return redirect(routes.MilestoneApp.milestone(userName, projectName, newMilestone.id));
        }
    }

    /**
     * Checks if there is a milestone that has the same name in {@code project}
     * If there is a milestone that has the same name, the method puts duplicate milestone name error message to the flash scope.
     *
     * @param project
     * @param milestoneForm
     */
    private static void validate(Project project, Form<Milestone> milestoneForm) {
        if (!Milestone.isUniqueProjectIdAndTitle(project.id, milestoneForm.field("title").value())) {
            milestoneForm.reject("title", "milestone.title.duplicated");
            flash(Constants.WARNING, "milestone.title.duplicated");
        }
    }

    /**
     * Moves to the edit milestone page for the milestone that matches {@code milestoneId} in the project that has {@code userName} and {@code projectName}
     * when: GET /:user/:project/milestone/:id/editform
     *
     * If no project has been matched, the method returns {@link #notFound()}
     *
     * @param userName
     * @param projectName
     * @param milestoneId
     * @return
     */
    @With(AnonymousCheckAction.class)
    @IsAllowed(value = Operation.UPDATE, resourceType = ResourceType.MILESTONE)
    public static Result editMilestoneForm(String userName, String projectName, Long milestoneId) {
        Project project = Project.findByOwnerAndProjectName(userName, projectName);
        Milestone milestone = Milestone.findById(milestoneId);

        Form<Milestone> editForm = new Form<>(Milestone.class).fill(milestone);
        return ok(edit.render("title.editMilestone", editForm, milestoneId, project));
    }

    /**
     * Updates a milestone with {@code milestoneId} in the project that matches {@code userName} and {@code projectName}
     * when: POST /:user/:project/milestone/:id/edit
     *
     * After changing the milestone name, check again if there’s no duplicate name.
     * If there’s no project that matches the given condition, it returns {@link #notFound()}.
     *
     * @param userName
     * @param projectName
     * @param milestoneId
     * @return
     */
    @Transactional
    @IsAllowed(value = Operation.UPDATE, resourceType = ResourceType.MILESTONE)
    public static Result editMilestone(String userName, String projectName, Long milestoneId) {
        Project project = Project.findByOwnerAndProjectName(userName, projectName);
        Form<Milestone> milestoneForm = new Form<>(Milestone.class).bindFromRequest();
        Milestone original = Milestone.findById(milestoneId);

        if(!original.title.equals(milestoneForm.field("title").value())) {
            validate(project, milestoneForm);
        }
        if (milestoneForm.hasErrors()) {
            return ok(edit.render("title.editMilestone", milestoneForm, milestoneId, project));
        } else {
            Milestone existingMilestone = Milestone.findById(milestoneId);
            Milestone milestone = milestoneForm.get();

            if (milestone.contents == null) {
                return status(REQUEST_ENTITY_TOO_LARGE,
                        ErrorViews.RequestTextEntityTooLarge.render());
            }

            milestone.dueDate = JodaDateUtil.lastSecondOfDay(milestone.dueDate);
            existingMilestone.updateWith(milestone);
            Attachment.moveAll(UserApp.currentUser().asResource(), existingMilestone.asResource());
            return redirect(routes.MilestoneApp.milestone(userName, projectName, existingMilestone.id));
        }
    }

    /**
     * Deletes the milestone with {@code milestoneId} in the project that matches {@code userName} and {@code projectName}
     * when: GET /:user/:project/milestone/:id/delete
     *
     * If no project has been matched, the method returns {@link #notFound()}
     * If the project id doesn’t match up with the project id of the milestone, it returns {@link #internalServerError()}.
     *
     * @param userName
     * @param projectName
     * @param id
     * @return
     */
    @Transactional
    @IsAllowed(value = Operation.DELETE, resourceType = ResourceType.MILESTONE)
    public static Result deleteMilestone(String userName, String projectName, Long id) {
        Project project = Project.findByOwnerAndProjectName(userName, projectName);
        Milestone milestone = Milestone.findById(id);

        if(!project.id.equals(milestone.project.id)) {
            return internalServerError();
        }
        milestone.delete();

        // IF this is response to XHR, return 204 No Content with a location header
        if(HttpUtil.isRequestedWithXHR(request())){
            response().setHeader("Location", routes.MilestoneApp.milestones(userName, projectName).toString());
            return status(204);
        }

        return redirect(routes.MilestoneApp.milestones(userName, projectName));
    }

    /**
     * Changes the status of the milestone with {@code milestoneId} into ‘unsolved’.
     *
     * @param userName
     * @param projectName
     * @param id
     * @return
     */
    @Transactional
    @IsAllowed(value = Operation.UPDATE, resourceType = ResourceType.MILESTONE)
    public static Result open(String userName, String projectName, Long id) {
        Milestone milestone = Milestone.findById(id);
        milestone.open();
        return redirect(routes.MilestoneApp.milestone(userName, projectName, id));
    }

    /**
     * Changes the status of the milestone with {@code milestoneId} into ‘solved’.
     *
     * @param userName
     * @param projectName
     * @param id
     * @return
     */
    @Transactional
    @IsAllowed(value = Operation.UPDATE, resourceType = ResourceType.MILESTONE)
    public static Result close(String userName, String projectName, Long id) {
        Milestone milestone = Milestone.findById(id);
        milestone.close();
        return redirect(routes.MilestoneApp.milestone(userName, projectName, id));
    }

    /**
     * Retrieves detailed information about the milestone that has {@code milestoneId}.
     * when: GET /:user/:project/milestone/:id
     *
     * @param userName
     * @param projectName
     * @param id
     * @return
     */
    @IsAllowed(value = Operation.READ, resourceType = ResourceType.MILESTONE)
    public static Result milestone(String userName, String projectName, Long id) {
        Project project = Project.findByOwnerAndProjectName(userName, projectName);
        Milestone milestone = Milestone.findById(id);

        String paramState = request().getQueryString("state");
        State state = State.getValue(paramState);
        UserApp.currentUser().visits(project);
        return ok(view.render(milestone.title, milestone, project, state));
    }
}
