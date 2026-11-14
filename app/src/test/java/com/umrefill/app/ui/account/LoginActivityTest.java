package com.umrefill.app.ui.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.umrefill.app.data.ServiceLocator;
import com.umrefill.app.data.entity.User;
import com.umrefill.app.data.entity.UserRole;
import com.umrefill.app.util.SessionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for FR1.1 & FR1.3:
 * - UM email validation (@um.edu.my and @siswa.um.edu.my)
 * - Login authentication for admin and student
 * - SessionManager login/logout role resolution
 */
public class LoginActivityTest {

    @Before
    public void setUp() {
        SessionManager.logout();
    }

    @After
    public void tearDown() {
        SessionManager.logout();
    }

    @Test
    public void validUmStaffEmailReturnsTrue() {
        assertTrue(LoginActivity.isValidUmEmail("admin@um.edu.my"));
        assertTrue(LoginActivity.isValidUmEmail("lecturer.cs@um.edu.my"));
        assertTrue(LoginActivity.isValidUmEmail("ADMIN@UM.EDU.MY"));
        assertTrue(LoginActivity.isValidUmEmail("  admin@um.edu.my  "));
    }

    @Test
    public void validUmStudentEmailReturnsTrue() {
        assertTrue(LoginActivity.isValidUmEmail("student@siswa.um.edu.my"));
        assertTrue(LoginActivity.isValidUmEmail("wong123@siswa.um.edu.my"));
        assertTrue(LoginActivity.isValidUmEmail("STUDENT@SISWA.UM.EDU.MY"));
    }

    @Test
    public void nonUmEmailReturnsFalse() {
        assertFalse(LoginActivity.isValidUmEmail("user@gmail.com"));
        assertFalse(LoginActivity.isValidUmEmail("user@yahoo.com"));
        assertFalse(LoginActivity.isValidUmEmail("user@um.edu.my.attacker.com"));
        assertFalse(LoginActivity.isValidUmEmail("user@siswa.um.edu.my.attacker.com"));
        assertFalse(LoginActivity.isValidUmEmail("user@subdomain.um.edu.my"));
    }

    @Test
    public void malformedEmailReturnsFalse() {
        assertFalse(LoginActivity.isValidUmEmail(null));
        assertFalse(LoginActivity.isValidUmEmail(""));
        assertFalse(LoginActivity.isValidUmEmail("   "));
        assertFalse(LoginActivity.isValidUmEmail("@um.edu.my"));
        assertFalse(LoginActivity.isValidUmEmail("@siswa.um.edu.my"));
        assertFalse(LoginActivity.isValidUmEmail("admin@um.edu.my@extra"));
        assertFalse(LoginActivity.isValidUmEmail("admin um@um.edu.my"));
    }

    @Test
    public void adminLoginSucceedsAndSetsAdminSession() {
        User admin = ServiceLocator.users().login("admin@um.edu.my", "admin123");
        assertNotNull(admin);
        assertEquals(UserRole.ADMIN, admin.role);

        SessionManager.login(admin);
        assertTrue(SessionManager.isLoggedIn());
        assertTrue(SessionManager.isAdmin());
        assertEquals("admin@um.edu.my", SessionManager.currentUser().email);
    }

    @Test
    public void studentLoginSucceedsAndSetsUserSession() {
        User student = ServiceLocator.users().login("student@siswa.um.edu.my", "student123");
        assertNotNull(student);
        assertEquals(UserRole.USER, student.role);

        SessionManager.login(student);
        assertTrue(SessionManager.isLoggedIn());
        assertFalse(SessionManager.isAdmin());
        assertEquals("student@siswa.um.edu.my", SessionManager.currentUser().email);
    }

    @Test
    public void loginFailsWithWrongPassword() {
        User user = ServiceLocator.users().login("admin@um.edu.my", "wrongpassword");
        assertNull(user);
        assertFalse(SessionManager.isLoggedIn());
    }

    @Test
    public void loginFailsWithNonExistentAccount() {
        User user = ServiceLocator.users().login("nonexistent@um.edu.my", "admin123");
        assertNull(user);
        assertFalse(SessionManager.isLoggedIn());
    }

    @Test
    public void logoutClearsSession() {
        User admin = ServiceLocator.users().login("admin@um.edu.my", "admin123");
        assertNotNull(admin);
        SessionManager.login(admin);
        assertTrue(SessionManager.isLoggedIn());
        assertTrue(SessionManager.isAdmin());

        SessionManager.logout();
        assertFalse(SessionManager.isLoggedIn());
        assertFalse(SessionManager.isAdmin());
        assertNull(SessionManager.currentUser());
    }
}
