package models;

import models.enumeration.ResourceType;
import models.resource.Resource;

import javax.persistence.*;

/**
 * 이슈의 댓글
 */
@Entity
public class IssueComment extends Comment {
    private static final long serialVersionUID = 1L;
    public static Finder<Long, IssueComment> find = new Finder<Long, IssueComment>(Long.class, IssueComment.class);

    @ManyToOne
    public Issue issue;

    /**
     * @see Comment#getParent()
     */
    public AbstractPosting getParent() {
        return issue;
    }

    /**
     * @see Comment#asResource()
     */
    public Resource asResource() {
        return new Resource() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public Project getProject() {
                return issue.project;
            }

            @Override
            public ResourceType getType() {
                return ResourceType.ISSUE_COMMENT;
            }

            @Override
            public Long getAuthorId() {
                return authorId;
            }
        };
    }
}
