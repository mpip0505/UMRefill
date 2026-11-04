package com.umrefill.app.data.fake;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.umrefill.app.data.entity.User;
import com.umrefill.app.data.entity.UserRole;
import com.umrefill.app.data.repo.UserRepository;
import com.umrefill.app.util.PasswordUtils;

import java.util.Locale;

public class FakeUserRepository implements UserRepository {

    public FakeUserRepository() {
        FakeStore.ensureSeeded();
    }

    @Nullable
    @Override
    public User login(@NonNull String email, @NonNull String password) {
        User u = findByEmail(email);
        if (u == null) return null;
        return PasswordUtils.matches(password, u.salt, u.passwordHash) ? u : null;
    }

    @Override
    public long register(@NonNull String email, @NonNull String password, @NonNull String displayName) {
        if (findByEmail(email) != null) return -1;
        User u = new User();
        u.userId = FakeStore.newId();
        u.email = email.trim().toLowerCase(Locale.ROOT);
        u.salt = PasswordUtils.newSalt();
        u.passwordHash = PasswordUtils.hash(password, u.salt);
        u.displayName = displayName;
        u.role = UserRole.USER;
        FakeStore.users.add(u);
        return u.userId;
    }

    @Override
    public boolean resetPassword(@NonNull String email, @NonNull String newPassword) {
        User u = findByEmail(email);
        if (u == null) return false;
        u.salt = PasswordUtils.newSalt();
        u.passwordHash = PasswordUtils.hash(newPassword, u.salt);
        return true;
    }

    @Nullable
    @Override
    public User findByEmail(@NonNull String email) {
        String wanted = email.trim().toLowerCase(Locale.ROOT);
        for (User u : FakeStore.users) {
            if (u.email.equals(wanted)) return u;
        }
        return null;
    }
}
