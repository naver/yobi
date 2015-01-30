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
package models;

import com.avaje.ebean.Page;
import models.enumeration.SearchType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SearchResult {

    private String keyword;
    private SearchType searchType;

    private long usersCount;
    private long projectsCount;
    private long issuesCount;
    private long postsCount;
    private long milestonesCount;
    private long issueCommentsCount;
    private long postCommentsCount;
    private long reviewsCount;

    private List<User> users;
    private List<Project> projects;
    private List<Issue> issues;
    private List<Posting> posts;
    private List<Milestone> milestones;
    private List<IssueComment> issueComments;
    private List<PostingComment> postComments;
    private List<ReviewComment> reviews;

    private long totalHit;
    private long totalPageCount;

    public List<String> makeSnippets(String contents, int threshold) {
        String lowerCaseContents = contents.toLowerCase();
        String lowerCaseKeyword = keyword.toLowerCase();

        LinkedList<BeginAndEnd> beginAndEnds = new LinkedList<>();
        List<Integer> indexes = findIndexes(lowerCaseContents, lowerCaseKeyword); // 6, 40

        for(int i = 0 ; i < indexes.size() ; i++) {
            int currentIndex = indexes.get(i);
            int beginIndex = beginIndex(currentIndex, threshold);
            int endIndex = endIndex(currentIndex + lowerCaseKeyword.length(), lowerCaseContents.length(), threshold);
            BeginAndEnd thisOne = new BeginAndEnd(beginIndex, endIndex);
            if(i == 0) {
                beginAndEnds.push(thisOne);
            } else {
                BeginAndEnd latestOne = beginAndEnds.peek();
                if(latestOne.getEndIndex() >= thisOne.getBeginIndex()) {
                    BeginAndEnd mergedOne = new BeginAndEnd(latestOne.getBeginIndex(), thisOne.getEndIndex());
                    beginAndEnds.pop();
                    beginAndEnds.push(mergedOne);
                } else {
                    beginAndEnds.push(thisOne);
                }
            }
        }

        Collections.reverse(beginAndEnds);

        List<String> snippets = new ArrayList<>();
        for(BeginAndEnd bae : beginAndEnds) {
            snippets.add(contents.substring(bae.beginIndex, bae.endIndex));
        }

        return snippets;
    }

    private List<Integer> findIndexes(String contents, String keyword) {
        List<Integer> indexes = new ArrayList<>();
        int index = contents.indexOf(keyword);
        while (index != -1) {
            indexes.add(index);
            index = contents.indexOf(keyword, index + keyword.length());
        }
        return indexes;
    }

    private int beginIndex(int index, int threshold) {
        return index < threshold ? 0 : index - threshold;
    }

    private int endIndex(int keywordEndIndex, int contentLength, int threshold) {
        int endIndex = keywordEndIndex + threshold;
        return endIndex < contentLength ? endIndex : contentLength;
    }

    public void updateSearchType() {
        if(!(this.searchType == SearchType.AUTO)) {
            return;
        }

        if (getIssuesCount() > 0) {
            setSearchType(SearchType.ISSUE);
            return;
        }

        if (getUsersCount() > 0) {
            setSearchType(SearchType.USER);
            return;
        }

        if (getProjectsCount() > 0) {
            setSearchType(SearchType.PROJECT);
            return;
        }

        if (getPostsCount() > 0) {
            setSearchType(SearchType.POST);
            return;
        }

        if (getMilestonesCount() > 0) {
            setSearchType(SearchType.MILESTONE);
            return;
        }

        if (getIssueCommentsCount() > 0) {
            setSearchType(SearchType.ISSUE_COMMENT);
            return;
        }

        if (getPostCommentsCount() > 0) {
            setSearchType(SearchType.POST_COMMENT);
            return;
        }

        if (getReviewsCount() > 0) {
            setSearchType(SearchType.REVIEW);
            return;
        }

        setSearchType(SearchType.ISSUE);
    }

    public void updateTotalPageCount(int pageSize) {
        if (this.totalHit <= pageSize) {
            setTotalPageCount(1l);
        } else {
            setTotalPageCount(this.totalHit / pageSize + 1);
        }
    }

    private class BeginAndEnd {
        int beginIndex;
        int endIndex;

        BeginAndEnd(int beginIndex, int endIndex) {
            this.beginIndex = beginIndex;
            this.endIndex = endIndex;
        }

        public int getBeginIndex() {
            return beginIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        @Override
        public String toString() {
            return "BeginAndEnd{" +
                    "beginIndex=" + beginIndex +
                    ", endIndex=" + endIndex +
                    '}';
        }
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public long getUsersCount() {
        return usersCount;
    }

    public void setUsersCount(long usersCount) {
        this.usersCount = usersCount;
    }

    public long getProjectsCount() {
        return projectsCount;
    }

    public void setProjectsCount(long projectsCount) {
        this.projectsCount = projectsCount;
    }

    public long getIssuesCount() {
        return issuesCount;
    }

    public void setIssuesCount(long issuesCount) {
        this.issuesCount = issuesCount;
    }

    public long getPostsCount() {
        return postsCount;
    }

    public void setPostsCount(long postsCount) {
        this.postsCount = postsCount;
    }

    public long getMilestonesCount() {
        return milestonesCount;
    }

    public void setMilestonesCount(long milestonesCount) {
        this.milestonesCount = milestonesCount;
    }

    public long getIssueCommentsCount() {
        return issueCommentsCount;
    }

    public void setIssueCommentsCount(long issueCommentsCount) {
        this.issueCommentsCount = issueCommentsCount;
    }

    public long getPostCommentsCount() {
        return postCommentsCount;
    }

    public void setPostCommentsCount(long postCommentsCount) {
        this.postCommentsCount = postCommentsCount;
    }

    public long getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(long reviewsCount) {
        this.reviewsCount = reviewsCount;
    }


    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
        this.totalHit = this.usersCount;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
        this.totalHit = this.projectsCount;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public void setIssues(List<Issue> issues) {
        this.issues = issues;
        this.totalHit = this.issuesCount;
    }

    public List<Posting> getPosts() {
        return posts;
    }

    public void setPosts(List<Posting> posts) {
        this.posts = posts;
        this.totalHit = this.postsCount;
    }

    public List<Milestone> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<Milestone> milestones) {
        this.milestones = milestones;
        this.totalHit = this.milestonesCount;
    }

    public List<IssueComment> getIssueComments() {
        return issueComments;
    }

    public void setIssueComments(List<IssueComment> issueComments) {
        this.issueComments = issueComments;
        this.totalHit = this.issueCommentsCount;
    }

    public List<PostingComment> getPostComments() {
        return postComments;
    }

    public void setPostComments(List<PostingComment> postComments) {
        this.postComments = postComments;
        this.totalHit = this.postCommentsCount;
    }

    public List<ReviewComment> getReviews() {
        return reviews;
    }

    public void setReviews(List<ReviewComment> reviews) {
        this.reviews = reviews;
        this.totalHit = this.reviewsCount;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public long getTotalPageCount() {
        return totalPageCount;
    }

    public void setTotalPageCount(long totalPageCount) {
        this.totalPageCount = totalPageCount;
    }

    public long getTotalHit() {
        return totalHit;
    }

    public void setTotalHit(long totalHit) {
        this.totalHit = totalHit;
    }
}
