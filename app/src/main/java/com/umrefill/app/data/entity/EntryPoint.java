package com.umrefill.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A lift, staircase or entrance the user can choose as "I am here" for the nearest-point search (FR3.2). */
@Entity(tableName = "entry_point",
        foreignKeys = @ForeignKey(entity = Floor.class, parentColumns = "floorId",
                childColumns = "floorId", onDelete = ForeignKey.CASCADE),
        indices = @Index("floorId"))
public class EntryPoint {
    @PrimaryKey(autoGenerate = true)
    public long entryId;

    public long floorId;

    @NonNull
    public String name = "";

    /** Position on the floor plan image, 0.0 (left/top) to 1.0 (right/bottom). */
    public float xPct;
    public float yPct;

    public EntryPoint() {}

    @Ignore
    public EntryPoint(long entryId, long floorId, @NonNull String name, float xPct, float yPct) {
        this.entryId = entryId;
        this.floorId = floorId;
        this.name = name;
        this.xPct = xPct;
        this.yPct = yPct;
    }
}
