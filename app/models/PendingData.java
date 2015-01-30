package models;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Keeun Baik
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"dataType", "dataId", "dataOperation"}))
public class PendingData extends Model {

    private static final long serialVersionUID = 1L;

    public static final Finder<Long, PendingData> find = new Finder<>(Long.class, PendingData.class);

    @Id
    public Long id;

    @Enumerated(EnumType.STRING)
    public DataType dataType;

    @Enumerated(EnumType.STRING)
    public DataOperation dataOperation;

    public long dataId;

    public static void add(Long dataId, DataType dataType, DataOperation dataOperation) {
        PendingData newPendingData = new PendingData();
        newPendingData.dataId = dataId;
        newPendingData.dataType = dataType;
        newPendingData.dataOperation = dataOperation;
        newPendingData.save();
    }

    public static enum DataType {
        USER, PROJECT, ISSUE, MILESTONE, POSTING, ISSUE_COMMENT, POSTING_COMMENT, REVIEW
    }

    public static enum DataOperation {
        INDEX, DELETE, UPDATE;
    }

}
