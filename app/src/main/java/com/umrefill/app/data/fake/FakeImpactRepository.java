package com.umrefill.app.data.fake;

import androidx.annotation.NonNull;

import com.umrefill.app.data.entity.Faculty;
import com.umrefill.app.data.entity.Floor;
import com.umrefill.app.data.entity.RefillLog;
import com.umrefill.app.data.entity.RefillPoint;
import com.umrefill.app.data.repo.ImpactRepository;

import java.util.LinkedHashMap;
import java.util.Map;

public class FakeImpactRepository implements ImpactRepository {

    public FakeImpactRepository() {
        FakeStore.ensureSeeded();
    }

    @Override
    public void logRefill(long userId, long pointId) {
        RefillLog log = new RefillLog();
        log.logId = FakeStore.newId();
        log.userId = userId;
        log.pointId = pointId;
        log.loggedAt = System.currentTimeMillis();
        FakeStore.refillLogs.add(log);
    }

    @Override
    public int getRefillCount(long userId) {
        int count = 0;
        for (RefillLog log : FakeStore.refillLogs) {
            if (log.userId == userId) count++;
        }
        return count;
    }

    @NonNull
    @Override
    public Map<String, Integer> getRefillTotalsByFaculty() {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (Faculty f : FakeStore.faculties) totals.put(f.shortCode, 0);
        for (RefillLog log : FakeStore.refillLogs) {
            String code = facultyCodeOfPoint(log.pointId);
            if (code != null) totals.put(code, totals.get(code) + 1);
        }
        return totals;
    }

    private static String facultyCodeOfPoint(long pointId) {
        for (RefillPoint p : FakeStore.points) {
            if (p.pointId != pointId) continue;
            for (Floor fl : FakeStore.floors) {
                if (fl.floorId != p.floorId) continue;
                for (Faculty f : FakeStore.faculties) {
                    if (f.facultyId == fl.facultyId) return f.shortCode;
                }
            }
        }
        return null;
    }
}
