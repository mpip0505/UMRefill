package com.umrefill.app.data.repo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.umrefill.app.data.entity.User;

/** Local accounts (Module 1). Blocking methods: see RefillPointRepository. */
public interface UserRepository {
    /** Returns the user if the email and password match, otherwise null. */
    @Nullable User login(@NonNull String email, @NonNull String password);

    /** Returns the new userId, or -1 if the email is already registered. */
    long register(@NonNull String email, @NonNull String password, @NonNull String displayName);

    /** FR1.2. Returns false if no account has this email. */
    boolean resetPassword(@NonNull String email, @NonNull String newPassword);

    @Nullable User findByEmail(@NonNull String email);
}
