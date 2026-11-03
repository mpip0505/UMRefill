package com.umrefill.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "faculty")
public class Faculty {
    @PrimaryKey(autoGenerate = true)
    public long facultyId;

    @NonNull
    public String name = "";

    /** Short label shown in lists, e.g. "FSKTM". */
    @NonNull
    public String shortCode = "";

    public Faculty() {}

    @Ignore
    public Faculty(long facultyId, @NonNull String name, @NonNull String shortCode) {
        this.facultyId = facultyId;
        this.name = name;
        this.shortCode = shortCode;
    }
}
