package com.umrefill.app.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "refill_point",
        foreignKeys = @ForeignKey(entity = Floor.class, parentColumns = "floorId",
                childColumns = "floorId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("floorId"), @Index(value = "qrCode", unique = true)})
public class RefillPoint {
    @PrimaryKey(autoGenerate = true)
    public long pointId;

    public long floorId;

    @NonNull
    public String name = "";

    /** One of {@link PointType}. */
    @NonNull
    public String type = PointType.NORMAL;

    /** One of {@link PointStatus}. */
    @NonNull
    public String status = PointStatus.WORKING;

    /** Position on the floor plan image, 0.0 (left/top) to 1.0 (right/bottom). */
    public float xPct;
    public float yPct;

    /** Short direction note, e.g. "Left of the lifts". */
    @NonNull
    public String locationNote = "";

    /** Epoch millis of the last report or admin confirmation. */
    public long lastConfirmedAt;

    /** Text printed in the station's QR sticker, e.g. "UMR-FSKTM-G-01". */
    @Nullable
    public String qrCode;

    /** Reachable from a wheelchair (filter in FR2.3). */
    public boolean accessible;

    public RefillPoint() {}

    @Ignore
    public RefillPoint(long pointId, long floorId, @NonNull String name, @NonNull String type,
                       @NonNull String status, float xPct, float yPct, @NonNull String locationNote,
                       long lastConfirmedAt, @Nullable String qrCode, boolean accessible) {
        this.pointId = pointId;
        this.floorId = floorId;
        this.name = name;
        this.type = type;
        this.status = status;
        this.xPct = xPct;
        this.yPct = yPct;
        this.locationNote = locationNote;
        this.lastConfirmedAt = lastConfirmedAt;
        this.qrCode = qrCode;
        this.accessible = accessible;
    }
}
