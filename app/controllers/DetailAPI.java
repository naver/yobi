package controllers;

import java.util.List;

import models.Comment;
import models.Issue;
import models.Project;
import models.User;

import org.codehaus.jackson.node.ObjectNode;

import play.libs.Json;
import models.*;
import models.enumeration.Operation;

import org.apache.commons.lang3.StringUtils;

import play.i18n.Messages;
import play.mvc.Result;
import utils.*;


public class DetailAPI extends AbstractPostingApp
{

	/**
	 *유저ID, 프로젝트명, 이슈의 번호를 받아 해당 이슈의 상세정보를 json형식으로 반환한다.
	 **/
    public static Result detailAPI(String ownerName, String projectName, Long number) 
    {
        //입력받은 유저ID가 생성한 프로젝트를 찾아서 Project class를 생성
        Project project = Project.findByOwnerAndProjectName(ownerName, projectName);
        //생성한 project에서 입력받은 number에 해당하는 이슈를 찾아서 Issue class 생성
        Issue issueInfo = Issue.findByNumber(project, number);
        //이슈 작성자의 정보
        User author = User.findByLoginId(ownerName);

        response().setHeader("Vary", "Accept");

        if (issueInfo == null) {
                ObjectNode result = Json.newObject();
                result.put("title", number);
                result.put("body", Messages.get("error.notfound.issue"));
                return ok(result);
        }

        if (!AccessControl.isAllowed(UserApp.currentUser(), issueInfo.asResource(), Operation.READ)) {
            return forbidden(ErrorViews.Forbidden.render("error.forbidden", project));
        }

        for (IssueLabel label: issueInfo.labels) {
            label.refresh();
        }		
        
        
        UserApp.currentUser().visits(project);
               
        ObjectNode result = Json.newObject();
        result.put("id", issueInfo.getNumber());
        result.put("title", issueInfo.title);
        result.put("author",author.name);
        result.put("state", issueInfo.state.toString());
        result.put("body", StringUtils.abbreviate(issueInfo.body, 1000));
        result.put("createdDate", issueInfo.createdDate.toString());
        result.put("link", routes.IssueApp.issue(project.owner, project.name, issueInfo.getNumber()).toString());
        return ok(result);
    }
}
