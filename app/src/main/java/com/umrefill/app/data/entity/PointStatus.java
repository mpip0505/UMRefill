package com.umrefill.app.data.entity;

/** Allowed values for {@link RefillPoint#status} and {@link Report#status}. Stored as plain strings in Room. */
public final class PointStatus {
    public static final String WORKING = "WORKING";
    public static final String BROKEN = "BROKEN";
    public static final String EMPTY = "EMPTY";
    public static final String DIRTY = "DIRTY";
    public static final String OUT_OF_SERVICE = "OUT_OF_SERVICE";

    private PointStatus() {}
}
