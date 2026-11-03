package com.umrefill.app.data.fake;

import com.umrefill.app.data.entity.EntryPoint;
import com.umrefill.app.data.entity.Faculty;
import com.umrefill.app.data.entity.Floor;
import com.umrefill.app.data.entity.PointStatus;
import com.umrefill.app.data.entity.PointType;
import com.umrefill.app.data.entity.RefillLog;
import com.umrefill.app.data.entity.RefillPoint;
import com.umrefill.app.data.entity.Report;
import com.umrefill.app.data.entity.User;
import com.umrefill.app.data.entity.UserRole;
import com.umrefill.app.util.PasswordUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory tables shared by the fake repositories, so every screen can be built before Room exists.
 *
 * PLACEHOLDER SAMPLE DATA: the faculties, floors and positions below are made up. Replace them
 * with the walk-through data (seed sheet) once it is collected.
 *
 * Demo accounts: admin@um.edu.my / admin123 (admin), student@siswa.um.edu.my / student123 (user).
 */
final class FakeStore {
    static final List<Faculty> faculties = new ArrayList<>();
    static final List<Floor> floors = new ArrayList<>();
    static final List<EntryPoint> entryPoints = new ArrayList<>();
    static final List<RefillPoint> points = new ArrayList<>();
    static final List<Report> reports = new ArrayList<>();
    static final List<User> users = new ArrayList<>();
    static final List<RefillLog> refillLogs = new ArrayList<>();

    private static long nextId = 1000;
    private static boolean seeded = false;

    private FakeStore() {}

    static synchronized long newId() {
        return nextId++;
    }

    static synchronized void ensureSeeded() {
        if (seeded) return;
        seeded = true;
        long now = System.currentTimeMillis();
        long hour = 60L * 60L * 1000L;

        faculties.add(new Faculty(1, "Faculty of Computer Science and Information Technology", "FSKTM"));
        faculties.add(new Faculty(2, "Faculty of Engineering", "ENG"));

        floors.add(new Floor(1, 1, 0, "Ground floor", null));
        floors.add(new Floor(2, 1, 1, "Level 1", null));
        floors.add(new Floor(3, 1, 2, "Level 2", null));
        floors.add(new Floor(4, 2, 0, "Ground floor", null));
        floors.add(new Floor(5, 2, 1, "Level 1", null));

        for (long floorId = 1; floorId <= 5; floorId++) {
            entryPoints.add(new EntryPoint(floorId * 10 + 1, floorId, "Main lifts", 0.50f, 0.50f));
            entryPoints.add(new EntryPoint(floorId * 10 + 2, floorId, "Stairs A", 0.10f, 0.85f));
        }

        points.add(new RefillPoint(1, 1, "Cooler near the lifts", PointType.COLD, PointStatus.WORKING,
                0.58f, 0.45f, "Right of the main lifts", now - 2 * hour, "UMR-FSKTM-G-01", true));
        points.add(new RefillPoint(2, 1, "Tap by the cafe", PointType.NORMAL, PointStatus.BROKEN,
                0.85f, 0.20f, "Next to the cafe entrance", now - 26 * hour, "UMR-FSKTM-G-02", true));
        points.add(new RefillPoint(3, 2, "Filtered dispenser", PointType.FILTERED, PointStatus.WORKING,
                0.30f, 0.60f, "Outside lab 1", now - 5 * hour, "UMR-FSKTM-1-01", false));
        points.add(new RefillPoint(4, 3, "Cooler at corridor end", PointType.COLD, PointStatus.EMPTY,
                0.90f, 0.50f, "End of the east corridor", now - 48 * hour, "UMR-FSKTM-2-01", false));
        points.add(new RefillPoint(5, 4, "Lobby cooler", PointType.COLD, PointStatus.WORKING,
                0.50f, 0.30f, "Main lobby, by the notice board", now - hour, "UMR-ENG-G-01", true));
        points.add(new RefillPoint(6, 5, "Filtered tap", PointType.FILTERED, PointStatus.DIRTY,
                0.20f, 0.70f, "Near the west stairs", now - 72 * hour, "UMR-ENG-1-01", false));

        users.add(makeUser(1, "admin@um.edu.my", "admin123", "Admin", UserRole.ADMIN));
        users.add(makeUser(2, "student@siswa.um.edu.my", "student123", "Demo Student", UserRole.USER));

        Report r = new Report();
        r.reportId = 1;
        r.pointId = 2;
        r.userId = 2;
        r.status = PointStatus.BROKEN;
        r.note = "No water comes out";
        r.createdAt = now - 26 * hour;
        reports.add(r);

        addLog(1, 2, 1, now - 3 * hour);
        addLog(2, 2, 3, now - 6 * hour);
        addLog(3, 2, 5, now - 30 * hour);
    }

    private static User makeUser(long id, String email, String password, String name, String role) {
        User u = new User();
        u.userId = id;
        u.email = email;
        u.salt = PasswordUtils.newSalt();
        u.passwordHash = PasswordUtils.hash(password, u.salt);
        u.displayName = name;
        u.role = role;
        return u;
    }

    private static void addLog(long logId, long userId, long pointId, long at) {
        RefillLog log = new RefillLog();
        log.logId = logId;
        log.userId = userId;
        log.pointId = pointId;
        log.loggedAt = at;
        refillLogs.add(log);
    }
}
