package com.umrefill.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Local account. Passwords are never stored in plain text: see util.PasswordUtils. */
@Entity(tableName = "app_user", indices = @Index(value = "email", unique = true))
public class User {
    @PrimaryKey(autoGenerate = true)
    public long userId;

    @NonNull
    public String email = "";

    @NonNull
    public String passwordHash = "";

    @NonNull
    public String salt = "";

    /** One of {@link UserRole}. */
    @NonNull
    public String role = UserRole.USER;

    @NonNull
    public String displayName = "";
}
