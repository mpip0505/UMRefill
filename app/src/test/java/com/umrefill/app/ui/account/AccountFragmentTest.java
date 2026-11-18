package com.umrefill.app.ui.account;

import static org.junit.Assert.assertEquals;

import com.umrefill.app.R;
import com.umrefill.app.data.entity.UserRole;

import org.junit.Test;

/**
 * Unit tests for {@link AccountFragment} role mapping.
 */
public class AccountFragmentTest {

    @Test
    public void adminRoleMapsToAdminStringResource() {
        assertEquals(R.string.role_admin, AccountFragment.getRoleStringRes(UserRole.ADMIN));
        assertEquals(R.string.role_admin, AccountFragment.getRoleStringRes("admin"));
    }

    @Test
    public void userRoleMapsToUserStringResource() {
        assertEquals(R.string.role_user, AccountFragment.getRoleStringRes(UserRole.USER));
        assertEquals(R.string.role_user, AccountFragment.getRoleStringRes("user"));
    }

    @Test
    public void unknownOrNullRoleReturnsZero() {
        assertEquals(0, AccountFragment.getRoleStringRes("SUPERUSER"));
        assertEquals(0, AccountFragment.getRoleStringRes(null));
        assertEquals(0, AccountFragment.getRoleStringRes(""));
    }
}
