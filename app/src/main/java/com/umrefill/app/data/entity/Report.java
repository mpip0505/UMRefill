package com.umrefill.app.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A community report on a refill point (FR4.1). */
@Entity(tableName = "report",
        foreignKeys = {
                @ForeignKey(entity = RefillPoint.class, parentColumns = "pointId",
                        childColumns = "pointId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class, parentColumns = "userId",
                        childColumns = "userId", onDelete = ForeignKey.CASCADE)},
        indices = {@Index("pointId"), @Index("userId")})
public class Report {
    @PrimaryKey(autoGenerate = true)
    public long reportId;

    public long pointId;
    public long userId;

    /** One of {@link PointStatus}. */
    @NonNull
    public String status = PointStatus.WORKING;

    @Nullable
    public String note;

    /** Path in app internal storage, null if no photo. */
    @Nullable
    public String photoPath;

    /** Top label from the photo check (FR6.1), null if not checked. */
    @Nullable
    public String aiLabel;
    public float aiConfidence;

    public long createdAt;

    /** True once an admin has reviewed it (FR4.3). */
    public boolean resolved;

    @Nullable
    public Long resolvedBy;
}
