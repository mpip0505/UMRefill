package com.umrefill.app.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** One refill logged by a user (FR5.1, FR5.2). One refill = one plastic bottle avoided. */
@Entity(tableName = "refill_log",
        foreignKeys = {
                @ForeignKey(entity = User.class, parentColumns = "userId",
                        childColumns = "userId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = RefillPoint.class, parentColumns = "pointId",
                        childColumns = "pointId", onDelete = ForeignKey.CASCADE)},
        indices = {@Index("userId"), @Index("pointId")})
public class RefillLog {
    @PrimaryKey(autoGenerate = true)
    public long logId;

    public long userId;
    public long pointId;
    public long loggedAt;
}
