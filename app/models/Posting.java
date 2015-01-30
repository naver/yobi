/**
 * @author Ahn Hyeok Jun
 */

package models;

import models.enumeration.ResourceType;
import models.resource.Resource;
import search.DataSynchronizer;
import search.Indexable;
import utils.JodaDateUtil;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.avaje.ebean.Expr.eq;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "number"}))
public class Posting extends AbstractPosting implements Indexable {
    private static final long serialVersionUID = 5287703642071155249L;

    public static final Finder<Long, Posting> finder = new Finder<>(Long.class, Posting.class);

    public boolean notice;
    public boolean readme;

    @OneToMany(cascade = CascadeType.ALL)
    public List<PostingComment> comments;

    public Posting(Project project, User author, String title, String body) {
        super(project, author, title, body);
    }

    /**
     * @see models.Project#increaseLastPostingNumber()
     */
    @Override
    protected Long increaseNumber() {
        return Project.increaseLastPostingNumber(project.id);
    }

    protected void fixLastNumber() {
        Project.fixLastPostingNumber(project.id);
    }

    /**
     * @see models.AbstractPosting#computeNumOfComments()
     */
    public int computeNumOfComments() {
        return comments.size();
    }

    public Posting() {
        super();
    }

    @Override
    public Resource asResource() {
        return asResource(ResourceType.BOARD_POST);
    }

    public static List<Posting> findNotices(Project project) {
        return Posting.finder.where()
                .eq("project.id", project.id)
                .add(eq("notice", true))
                .order().desc("createdDate")
                .findList();
    }

    public static List<Posting> findRecentlyCreated(Project project, int size) {
        return Posting.finder.where()
                .eq("project.id", project.id)
                .order().desc("createdDate")
                .findPagingList(size).getPage(0)
                .getList();
    }

    public static List<Posting> findRecentlyCreatedByDaysAgo(Project project, int days) {
        return Posting.finder.where()
                .eq("project.id", project.id)
                .ge("createdDate", JodaDateUtil.before(days)).order().desc("createdDate").findList();
    }

    /**
     * @see models.AbstractPosting#getComments()
     */
    @Transient
    public List<? extends Comment> getComments() {
        Collections.sort(comments, Comment.comparator());
        return comments;
    }

    @Override
    public void checkLabels() {

    }

    public static Posting findByNumber(Project project, long number) {
        return AbstractPosting.findByNumber(finder, project, number);
    }

    public static int countPostings(Project project) {
        return finder.where().eq("project", project).findRowCount();
    }

    /**
     * use EBean save functionality directly
     * to prevent occurring select table lock
     */
    public void directSave(){
        super.directSave();
    }

    public static Posting findREADMEPosting(Project project) {
        return Posting.finder.where()
                .eq("project.id", project.id)
                .add(eq("readme", true))
                .findUnique();
    }

    @Override
    public String indexId() {
        return this.id.toString();
    }

    @Nonnull
    @Override
    public Map<String, Object> source() {
        Map<String, Object> source = new HashMap<>();
        if (this.title != null) {
            source.put("title", this.title);
        }
        if (this.body != null) {
            source.put("body", this.body);
        }
        if (this.createdDate != null) {
            source.put("created", this.createdDate);
        }
        if (this.project != null) {
            source.put("projectId", this.project.id);
        }
        if (this.authorId != number) {
            source.put("authorId", this.authorId);
        }
        return source;
    }

}
