package controllers;

import java.util.*;
import utils.LabelSearchUtil;

import models.Issue;
import models.Project;

import org.codehaus.jackson.node.ObjectNode;

import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.Page;

import play.data.Form;
import play.libs.Json;
import jxl.write.WriteException;
import models.*;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
 
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

        Integer itemsPerPage = getItemsPerPage();
        ExpressionList<Issue> el = searchCondition.asExpressionList(project);
        Page<Issue> issues = el.findPagingList(itemsPerPage).getPage(searchCondition.pageNum);

        return issuesAsJson_List(project, issues);
    }
	public static Result searchAPI(String state, int pageNum, String authorId, String assigneeId, String mentionId) throws WriteException, IOException {
int authorid, assigneeid, mentionid;
        //authorId가 0이 아닐경우는 내가 작성한 이슈를 검색하여 json형식으로 리턴해준다. 
        if(authorId.length()==0){
            authorid=0;
        }else{
            authorid=Integer.parseInt(authorId);
        }
        //assigneeId가 0이 아닐경우는 내가 담당된 이슈를 검색하여 json형식으로 리턴해준다.
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
        //mentionid가 0이 아닐경우는 내가 언급된 이슈를 검색하여 해당하는 이슈를 검색하여 json형식으로 리턴해준다.
        if(mentionid!=0){
            searchCondition.mentionId=(long)mentionid;
            el=searchCondition.asExpressionList();
            issues=el.findPagingList(itemsPerPage).getPage(searchCondition.pageNum);
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

    //unique id는 전체 프로젝트 내에서 해당 이슈의 id를 의미합니다. 
    //예를들어 test project의 1번 이슈는 1이라는 unique id와 1이라는 id가 붙지만
    //test2 project의 1번 이슈는 33이라는 unique id와 1이라는 id가 붙습니다.
    //json형식에서 제일 앞에 붙는 issue.id.toString()은 이 unique id를 나타냅니다.
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
            if(routes.IssueApp.issue(project.owner, project.name, issueId)==null){
                result.put("link", "");
            }else{
                result.put("link", routes.IssueApp.issue(project.owner, project.name, issueId).toString());
            }
            result.put("numofComments", NumOfComments);
            result.put("AssigneeID", issue.assigneeName());
            Set<IssueLabel> issueLabel= issue.getLabels();
            String issueLabelStr;
            if(!issueLabel.isEmpty()){
                labelArray=issueLabel.toArray();
                issueLabelStr= labelArray[0].toString();
            }else{
                issueLabelStr="No Label";
            }
            result.put("issueLabel", issueLabelStr);
            listData.put(issue.id.toString(), result);
        }
        return ok(listData);
    }

    //unique id는 전체 프로젝트 내에서 해당 이슈의 id를 의미합니다. 
    //예를들어 test project의 1번 이슈는 1이라는 unique id와 1이라는 id가 붙지만
    //test2 project의 1번 이슈는 33이라는 unique id와 1이라는 id가 붙습니다.
    //json형식에서 제일 앞에 붙는 issue.id.toString()은 이 unique id를 나타냅니다.
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
        		    if(issueId.equals(exceptId)){
        			    continue;
        		    }

        		    ObjectNode result = Json.newObject();
        		    result.put("id", issueId);
        		    result.put("title", issue.title);
        		    result.put("state", issue.state.toString());
        		    result.put("createdDate", issue.createdDate.toString());
        		    listData.put(issue.id.toString(), result);
                }
            }
            return ok(listData);
        }
        else if(authorId!=0){
            for (Issue issue : issueList){
        	    if(issue.authorId==authorId){
        		    Long issueId = issue.getNumber();
        		    if(issueId.equals(exceptId)){
        			    continue;
                    }

        		    ObjectNode result = Json.newObject();
        		    result.put("id", issueId);
        		    result.put("title", issue.title);
        		    result.put("state", issue.state.toString());
        		    result.put("createdDate", issue.createdDate.toString());
        		    listData.put(issue.id.toString(), result);
                }
            }
            return ok(listData);
        }
        else if(mentionId!=0){
            for (Issue issue : issueList){
        	        Long issueId = issue.getNumber();

        		    if(issueId.equals(exceptId)){
        			    continue;
        			}

        		    ObjectNode result = Json.newObject();
        		    result.put("id", issueId);
        		    result.put("title", issue.title);
        		    result.put("state", issue.state.toString());
        		    result.put("createdDate", issue.createdDate.toString());
        		    listData.put(issue.id.toString(), result);
            }
            return ok(listData);
        }
        return ok();
        
	}

}


 
