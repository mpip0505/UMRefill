package com.umrefill.app.util;

import androidx.annotation.Nullable;

import com.umrefill.app.data.entity.User;
import com.umrefill.app.data.entity.UserRole;

/**
 * Who is logged in. Kept in memory for now, so the app forgets the login when it is killed.
 * TODO (Dev D): persist the userId in SharedPreferences so the login survives restarts.
 */
public final class SessionManager {
    @Nullable
    private static User currentUser;

    private SessionManager() {}

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    @Nullable
    public static User currentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null && UserRole.ADMIN.equals(currentUser.role);
    }
}
