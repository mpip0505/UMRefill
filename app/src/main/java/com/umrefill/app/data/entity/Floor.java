package com.umrefill.app.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "floor",
        foreignKeys = @ForeignKey(entity = Faculty.class, parentColumns = "facultyId",
                childColumns = "facultyId", onDelete = ForeignKey.CASCADE),
        indices = @Index("facultyId"))
public class Floor {
    @PrimaryKey(autoGenerate = true)
    public long floorId;

    public long facultyId;

    /** Sort order: 0 = ground, 1 = level 1, -1 = basement. */
    public int level;

    /** Display label, e.g. "Ground floor", "Level 2". */
    @NonNull
    public String label = "";

    /** Drawable resource name of the floor plan image, e.g. "plan_fsktm_g". Null until traced. */
    @Nullable
    public String planImage;

    public Floor() {}

    @Ignore
    public Floor(long floorId, long facultyId, int level, @NonNull String label, @Nullable String planImage) {
        this.floorId = floorId;
        this.facultyId = facultyId;
        this.level = level;
        this.label = label;
        this.planImage = planImage;
    }
}
