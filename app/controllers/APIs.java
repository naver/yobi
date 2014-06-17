package controllers;

import static play.data.Form.form;
import play.*;
import java.util.*;
import play.mvc.*;
import utils.HttpUtil;
import utils.LabelSearchUtil;
import views.html.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import models.Issue;
import models.Milestone;
import models.Project;

import org.codehaus.jackson.node.ObjectNode;

import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.Page;

import play.data.Form;
import play.libs.Json;
import actions.NullProjectCheckAction;
import actions.AnonymousCheckAction;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.Page;

import controllers.MilestoneApp.MilestoneCondition;
import controllers.annotation.IsAllowed;
import controllers.annotation.IsCreatable;
import jxl.write.WriteException;
import models.*;
import models.enumeration.Direction;
import models.enumeration.Operation;
import models.enumeration.ResourceType;
import models.enumeration.State;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.codehaus.jackson.node.ObjectNode;
import play.api.templates.Html;
import play.data.Form;
import play.data.validation.ValidationError;
import play.db.ebean.Transactional;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import utils.*;
import views.html.issue.*;
import views.html.milestone.list;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import utils.HttpUtil;
import java.lang.*;
 
public class APIs extends AbstractPostingApp{
	private static final Integer ITEMS_PER_PAGE_MAX = 45;
	/**
	 * @param args
	 */
	public static Result issueAPI(String ownerName, String projectName, int pageNum) throws WriteException, IOException {
		Project project = Project.findByOwnerAndProjectName(ownerName, projectName);

        // SearchCondition from param
        Form<models.support.SearchCondition> issueParamForm = new Form<>(models.support.SearchCondition.class);
        models.support.SearchCondition searchCondition = issueParamForm.bindFromRequest().get();
        searchCondition.pageNum = pageNum - 1;
        searchCondition.labelIds.addAll(LabelSearchUtil.getLabelIds(request()));

        // determine pjax or json when requested with XHR
        /*if (HttpUtil.isRequestedWithXHR(request())) {
            format = HttpUtil.isPJAXRequest(request()) ? "pjax" : "json";
        }*/

        Integer itemsPerPage = getItemsPerPage();
        ExpressionList<Issue> el = searchCondition.asExpressionList(project);
        Page<Issue> issues = el.findPagingList(itemsPerPage).getPage(searchCondition.pageNum);

        return issuesAsJson_List(project, issues);
    }
	/*public static Result milestoneAPI(String userName, String projectName, int pageNum) throws IllegalArgumentException, IllegalAccessException{
		
		Project project = Project.findByOwnerAndProjectName(userName, projectName);
        MilestoneCondition mCondition = form(MilestoneCondition.class).bindFromRequest().get();

        List<Milestone> milestones = Milestone.findMilestones(project.id,
                State.getValue(mCondition.state),
                mCondition.orderBy,
                Direction.getValue(mCondition.orderDir));
        ObjectNode listData=Json.newObject();
        String iiii="";
        Object[] marray=milestones.toArray();
        int i,j;
        for(i=0;i<marray.length;i++){
        	java.lang.reflect.Field[] a = marray[i].getClass().getDeclaredFields();
        	for(j=0;j<a.length;j++){
        		iiii+=a[j].getName();
        		iiii+="/";
        		
        		
        		iiii+=a[j].get(marray[i]).toString();
        		iiii+="//";
        		iiii+=a[j].get(marray[i]).getClass().getTypeParameters();
        		iiii+="///";
        	}
        	iiii+=";;;";
        	//iiii+=marray[i].toString();
        }
        //listData.put("state", mCondition.state);
        //listData.put("milestone name", mCondition.)
        return ok(iiii);
        //return ok(list.render("title.milestoneList", milestones, project, mCondition));
	}*/
	public static Result searchAPI(String state, int pageNum, String authorId, String assigneeId, String mentionId) throws WriteException, IOException {
        Project project = null;
        // SearchCondition from param
        Form<models.support.SearchCondition> issueParamForm = new Form<>(models.support.SearchCondition.class);
        models.support.SearchCondition searchCondition = issueParamForm.bindFromRequest().get();
        if (hasNotConditions(searchCondition)) {
            searchCondition.assigneeId = UserApp.currentUser().id;
        }
        searchCondition.pageNum = pageNum - 1;

        

        Integer itemsPerPage = getItemsPerPage();
        ExpressionList<Issue> el = searchCondition.asExpressionList();
        Page<Issue> issues = el.findPagingList(itemsPerPage).getPage(searchCondition.pageNum);
        //String ids=assigneeId+authorId+mentionId;
		/*int len=authorId.length();
        String ids=""+len+"/"+"".length();
        return ok(ids);*/
        int authorid, assigneeid, mentionid;
        if(authorId.length()==0){
        	authorid=0;
        }else{
        	authorid=Integer.parseInt(authorId);
        }
        if(assigneeId.length()==0){
        	assigneeid=0;
        }else{
        	assigneeid=Integer.parseInt(assigneeId);
        }
        if(mentionId.length()==0){
        	mentionid=0;
        }else{
        	mentionid=Integer.parseInt(mentionId);
        }
        return issuesAsJson_Search(project, issues,assigneeid,authorid,mentionid);

    }
	private static boolean hasNotConditions(models.support.SearchCondition searchCondition) {
        return searchCondition.assigneeId == null && searchCondition.authorId == null && searchCondition.mentionId == null;
    }
	private static Integer getItemsPerPage(){
        Integer itemsPerPage = ITEMS_PER_PAGE;
        String amountStr = request().getQueryString("itemsPerPage");

        if(amountStr != null){ // or amount from query string
            try {
                itemsPerPage = Integer.parseInt(amountStr);
            } catch (NumberFormatException ignored){}
        }

        return Math.min(itemsPerPage, ITEMS_PER_PAGE_MAX);
    }
	private static Result issuesAsJson_List(Project project, Page<Issue> issues) {
        ObjectNode listData = Json.newObject();

        // 반환할 목록에서 제외할 이슈ID 를 exceptId 로 지정할 수 있다(QueryString)
        // 이슈 수정시 '비슷할 수 있는 이슈' 목록에서 현재 수정중인 이슈를 제외하기 위해 사용한다
        String exceptIdStr = request().getQueryString("exceptId");
        Long exceptId = -1L;

        if(!StringUtils.isEmpty(exceptIdStr)){
            try {
                exceptId = Long.parseLong(exceptIdStr);
            } catch(Exception e){
                return badRequest(listData);
            }
        }

        List<Issue> issueList = issues.getList(); // the list of entities for this page
        /*Object[] temp=issueList.toArray();
        String issueData=temp.toString();*/
        Object[] labelArray; 
        for (Issue issue : issueList){
            Long issueId = issue.getNumber();
            int NumOfComments=issue.computeNumOfComments();

            if(issueId.equals(exceptId)){
                continue;
            }

            ObjectNode result = Json.newObject();
            result.put("id", issueId);
            result.put("title", issue.title);
            result.put("state", issue.state.toString());
            result.put("createdDate", issue.createdDate.toString());
            result.put("link", routes.IssueApp.issue(project.owner, project.name, issueId).toString());
            result.put("numofComments", NumOfComments);
            /*if(issue.assignee!=null){
            	result.put("AssigneeID", issue.assigneeName());
            }else{
            	result.put("AssigneeID", "Null");
            }*/
            result.put("AssigneeID", issue.assigneeName());
            Set<IssueLabel> issueLabel= issue.getLabels();
            String issueLabelStr;
            if(!issueLabel.isEmpty()){
            	labelArray=issueLabel.toArray();
            	issueLabelStr= labelArray[0].toString();
            }else{
            	issueLabelStr="No Label";
            }
            /*for(i=0;i<labelArray.length;i++){
            	resul= resul + labelArray[i].toString() +"/"+labelArray.length;
            }*/
            
            result.put("issueLabel", issueLabelStr);
            listData.put(issue.id.toString(), result);
        }
        return ok(listData);
    }
	private static Result issuesAsJson_Search(Project project, Page<Issue> issues, int assigneeId, int authorId, int mentionId) {
        ObjectNode listData = Json.newObject();

        // 반환할 목록에서 제외할 이슈ID 를 exceptId 로 지정할 수 있다(QueryString)
        // 이슈 수정시 '비슷할 수 있는 이슈' 목록에서 현재 수정중인 이슈를 제외하기 위해 사용한다
        String exceptIdStr = request().getQueryString("exceptId");
        Long exceptId = -1L;

        if(!StringUtils.isEmpty(exceptIdStr)){
            try {
                exceptId = Long.parseLong(exceptIdStr);
            } catch(Exception e){
                return badRequest(listData);
            }
        }

        List<Issue> issueList = issues.getList(); // the list of entities for this page
        if(assigneeId!=0){
        
        	for (Issue issue : issueList){
        		if(issue.assignee.id==assigneeId){
        			Long issueId = issue.getNumber();
        			//int NumOfComments=issue.computeNumOfComments();

        			if(issueId.equals(exceptId)){
        				continue;
        			}

        			ObjectNode result = Json.newObject();
        			result.put("id", issueId);
        			result.put("title", issue.title);
        			result.put("state", issue.state.toString());
        			result.put("createdDate", issue.createdDate.toString());
        			//result.put("numofComments", NumOfComments);
        			//result.put("AssigneeID", issue.assigneeName());
        			
        			listData.put(issue.id.toString(), result);
        		}
        	}
        	return ok(listData);
        }
        else if(authorId!=0){
        	for (Issue issue : issueList){
        		if(issue.authorId==authorId){
        			Long issueId = issue.getNumber();
        			//int NumOfComments=issue.computeNumOfComments();

        			if(issueId.equals(exceptId)){
        				continue;
        			}

        			ObjectNode result = Json.newObject();
        			result.put("id", issueId);
        			result.put("title", issue.title);
        			result.put("state", issue.state.toString());
        			result.put("createdDate", issue.createdDate.toString());
        			//result.put("numofComments", NumOfComments);
        			//result.put("AssigneeID", issue.assigneeName());
        			
        			listData.put(issue.id.toString(), result);
        		}
        	}
        	return ok(listData);
        }
        /*else if(mentionId!=0){
        	for (Issue issue : issueList){
        		if(issue.==mentionId){
        			Long issueId = issue.getNumber();
        			int NumOfComments=issue.computeNumOfComments();

        			if(issueId.equals(exceptId)){
        				continue;
        			}

        			ObjectNode result = Json.newObject();
        			result.put("id", issueId);
        			result.put("title", issue.title);
        			result.put("state", issue.state.toString());
        			result.put("createdDate", issue.createdDate.toString());
        			//result.put("numofComments", NumOfComments);
        			//result.put("AssigneeID", issue.assigneeName());
        			
        			listData.put(issue.id.toString(), result);
        		}
        	}
        	return ok(listData);
        }*/
        return ok();
        
	}

}


 
