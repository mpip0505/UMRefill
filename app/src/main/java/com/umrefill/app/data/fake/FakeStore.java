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
 * PLACEHOLDER SAMPLE DATA: the FSKTM and ENG faculties, floors and positions below are made up.
 * Replace them with the walk-through data (seed sheet) once it is collected.
 * FBL floors (floorId 6-9) are seeded with surveyed coordinates calibrated to map_fbl_*.webp drawables.
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
        faculties.add(new Faculty(3, "Faculty of Languages and Linguistics", "FBL"));

        // Placeholder sample floors for FSKTM and ENG (to be updated with walk-through data)
        floors.add(new Floor(1, 1, 0, "Block A - Ground Floor", "fsktm_blk_a_g"));
        floors.add(new Floor(2, 1, 1, "Block A - Floor 1", "fsktm_blk_a_f1"));
        floors.add(new Floor(3, 1, 2, "Block A - Floor 2", "fsktm_blk_a_f2"));
        floors.add(new Floor(4, 2, 0, "Ground floor", null));
        floors.add(new Floor(5, 2, 1, "Level 1", null));

        // FBL Floors matched to res/drawable/map_fbl_*.webp (level is 0=Ground, 1=Floor 1 per building)
        floors.add(new Floor(6, 3, 0, "Anggerik · Ground Floor", "map_fbl_anggerik_ground"));
        floors.add(new Floor(7, 3, 1, "Anggerik · Floor 1", "map_fbl_anggerik_floor1"));
        floors.add(new Floor(8, 3, 0, "Bakawali · Ground Floor", "map_fbl_bakawali_ground"));
        floors.add(new Floor(9, 3, 1, "Bakawali · Floor 1", "map_fbl_bakawali_floor1"));

        for (long floorId = 1; floorId <= 5; floorId++) {
            entryPoints.add(new EntryPoint(floorId * 10 + 1, floorId, "Main lifts", 0.50f, 0.50f));
            entryPoints.add(new EntryPoint(floorId * 10 + 2, floorId, "Stairs A", 0.10f, 0.85f));
        }

        // FBL Floor 6: Anggerik Ground Floor Entry Points (Calibrated to map_fbl_anggerik_ground.webp)
        entryPoints.add(new EntryPoint(61, 6, "Main Entrance (Foyer)", 0.424f, 0.775f));
        entryPoints.add(new EntryPoint(62, 6, "Left Exit (Hunian Lendip / GL28)", 0.115f, 0.748f));
        entryPoints.add(new EntryPoint(63, 6, "Latar Siswa Stairs (Hunian Cendekia)", 0.171f, 0.495f));
        entryPoints.add(new EntryPoint(64, 6, "Right Exit (Hunian Santun / GL13)", 0.725f, 0.748f));
        entryPoints.add(new EntryPoint(65, 6, "North Stairs (Hunian Bajik / GR1)", 0.712f, 0.210f));

        // FBL Floor 7: Anggerik Floor 1 Entry Points (Calibrated to map_fbl_anggerik_floor1.webp)
        entryPoints.add(new EntryPoint(71, 7, "Center Stairs (Hunian Amal / FL22)", 0.498f, 0.815f));
        entryPoints.add(new EntryPoint(72, 7, "Bridge to Bakawali (Laluan Keluar)", 0.328f, 0.284f));
        entryPoints.add(new EntryPoint(73, 7, "Left Exit Stairs (FL30)", 0.160f, 0.810f));
        entryPoints.add(new EntryPoint(74, 7, "Right Exit Stairs (Surau Lelaki)", 0.870f, 0.810f));
        entryPoints.add(new EntryPoint(75, 7, "North Stairs (Hunian Tulus / FL1)", 0.880f, 0.145f));

        // FBL Floor 8: Bakawali Ground Floor Entry Points (Calibrated to map_fbl_bakawali_ground.webp)
        entryPoints.add(new EntryPoint(81, 8, "Main Entrance Stairs (Lobi)", 0.355f, 0.675f));
        entryPoints.add(new EntryPoint(82, 8, "Lobi Central Stairs (Bilik Suis)", 0.368f, 0.415f));
        entryPoints.add(new EntryPoint(83, 8, "West Stairs (Near TB17)", 0.092f, 0.415f));
        entryPoints.add(new EntryPoint(84, 8, "East Stairs (Near TB26 & OKU)", 0.632f, 0.415f));
        entryPoints.add(new EntryPoint(85, 8, "Perpustakaan Corridor", 0.670f, 0.472f));

        // FBL Floor 9: Bakawali Floor 1 Entry Points (Calibrated to map_fbl_bakawali_floor1.webp)
        entryPoints.add(new EntryPoint(91, 9, "SAC Foyer (Anda Di Sini / TS24)", 0.645f, 0.470f));
        entryPoints.add(new EntryPoint(92, 9, "Central Stairs (Near TS20 / SAC1)", 0.498f, 0.385f));
        entryPoints.add(new EntryPoint(93, 9, "West Stairs (Near TS2)", 0.102f, 0.385f));
        entryPoints.add(new EntryPoint(94, 9, "East Stairs (Near TS26)", 0.890f, 0.385f));

        // Placeholder sample refill points for FSKTM & ENG
        points.add(new RefillPoint(1, 1, "Cooler near the lifts", PointType.COLD, PointStatus.WORKING,
                0.58f, 0.45f, "Right of the main lifts", now - (2L * hour), "UMR-FSKTM-G-01", true));
        points.add(new RefillPoint(2, 1, "Tap by the cafe", PointType.NORMAL, PointStatus.BROKEN,
                0.85f, 0.20f, "Next to the cafe entrance", now - (26L * hour), "UMR-FSKTM-G-02", true));
        points.add(new RefillPoint(3, 2, "Filtered dispenser", PointType.FILTERED, PointStatus.WORKING,
                0.30f, 0.60f, "Outside lab 1", now - (5L * hour), "UMR-FSKTM-1-01", false));
        points.add(new RefillPoint(4, 3, "Cooler at corridor end", PointType.COLD, PointStatus.EMPTY,
                0.90f, 0.50f, "End of the east corridor", now - (48L * hour), "UMR-FSKTM-2-01", false));
        points.add(new RefillPoint(5, 4, "Lobby cooler", PointType.COLD, PointStatus.WORKING,
                0.50f, 0.30f, "Main lobby, by the notice board", now - hour, "UMR-ENG-G-01", true));
        points.add(new RefillPoint(6, 5, "Filtered tap", PointType.FILTERED, PointStatus.DIRTY,
                0.20f, 0.70f, "Near the west stairs", now - (72L * hour), "UMR-ENG-1-01", false));

        // FBL Floor 6 (Anggerik Ground Floor) — 3 Red Dots calibrated to map_fbl_anggerik_ground.webp
        points.add(new RefillPoint(7, 6, "Foyer Dispenser (Near GL19)", PointType.FILTERED, PointStatus.WORKING,
                0.456f, 0.718f, "At Foyer & Hunian Santun corridor above GL19", now - hour, "UMR-FBL-AG-01", true));
        points.add(new RefillPoint(8, 6, "Hunian Santun Toilet Cooler (GL13)", PointType.COLD, PointStatus.WORKING,
                0.642f, 0.718f, "Beside GL13 and Men's Toilet at Hunian Santun end", now - (2L * hour), "UMR-FBL-AG-02", true));
        points.add(new RefillPoint(9, 6, "Hunian Bistari / Bajik Cooler", PointType.FILTERED, PointStatus.WORKING,
                0.680f, 0.500f, "Between GR6 WC and GL6 WC along Hunian Bistari", now - (3L * hour), "UMR-FBL-AG-03", true));

        // FBL Floor 7 (Anggerik Floor 1) — 2 Red Dots calibrated to map_fbl_anggerik_floor1.webp
        points.add(new RefillPoint(10, 7, "Hunian Amal (Surau Perempuan) Dispenser", PointType.FILTERED, PointStatus.WORKING,
                0.285f, 0.785f, "Along Hunian Amal corridor beside FR25 and Surau Perempuan", now - (2L * hour), "UMR-FBL-AF1-01", true));
        points.add(new RefillPoint(11, 7, "Hunian Adab (FR10 Toilet) Cooler", PointType.COLD, PointStatus.WORKING,
                0.805f, 0.785f, "Corner of Hunian Adab & Hunian Insaf beside FR10 and FL13 Toilet", now - (4L * hour), "UMR-FBL-AF1-02", true));

        // FBL Floor 8 (Bakawali Ground Floor) — 1 Red Dot calibrated to map_fbl_bakawali_ground.webp
        points.add(new RefillPoint(12, 8, "Bakawali Lobi Dispenser (Near TB2)", PointType.FILTERED, PointStatus.WORKING,
                0.338f, 0.520f, "Inside Bakawali Lobi beside lecturer room TB2", now - (2L * hour), "UMR-FBL-BG-01", true));

        // FBL Floor 9 (Bakawali Floor 1) — 1 Red Dot calibrated to map_fbl_bakawali_floor1.webp
        points.add(new RefillPoint(13, 9, "West Wing Dispenser (Near TS1 & TS2)", PointType.COLD, PointStatus.WORKING,
                0.138f, 0.462f, "Along West lecturer corridor between TS1 and TS2 near toilets", now - hour, "UMR-FBL-BF1-01", true));

        users.add(makeUser(1, "admin@um.edu.my", "admin123", "Admin", UserRole.ADMIN));
        users.add(makeUser(2, "student@siswa.um.edu.my", "student123", "Demo Student", UserRole.USER));

        Report r = new Report();
        r.reportId = 1;
        r.pointId = 2;
        r.userId = 2;
        r.status = PointStatus.BROKEN;
        r.note = "No water comes out";
        r.createdAt = now - (26L * hour);
        reports.add(r);

        addLog(1, 2, 1, now - (3L * hour));
        addLog(2, 2, 3, now - (6L * hour));
        addLog(3, 2, 5, now - (30L * hour));
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