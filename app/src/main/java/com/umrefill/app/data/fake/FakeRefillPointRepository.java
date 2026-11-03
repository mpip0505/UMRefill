package com.umrefill.app.data.fake;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.umrefill.app.data.entity.EntryPoint;
import com.umrefill.app.data.entity.Faculty;
import com.umrefill.app.data.entity.Floor;
import com.umrefill.app.data.entity.RefillPoint;
import com.umrefill.app.data.repo.RefillPointRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FakeRefillPointRepository implements RefillPointRepository {

    public FakeRefillPointRepository() {
        FakeStore.ensureSeeded();
    }

    @NonNull
    @Override
    public List<Faculty> getFaculties() {
        return new ArrayList<>(FakeStore.faculties);
    }

    @NonNull
    @Override
    public List<Floor> getFloors(long facultyId) {
        List<Floor> result = new ArrayList<>();
        for (Floor f : FakeStore.floors) {
            if (f.facultyId == facultyId) result.add(f);
        }
        Collections.sort(result, (a, b) -> Integer.compare(a.level, b.level));
        return result;
    }

    @Nullable
    @Override
    public Floor getFloor(long floorId) {
        for (Floor f : FakeStore.floors) {
            if (f.floorId == floorId) return f;
        }
        return null;
    }

    @NonNull
    @Override
    public List<RefillPoint> getPoints(long floorId) {
        List<RefillPoint> result = new ArrayList<>();
        for (RefillPoint p : FakeStore.points) {
            if (p.floorId == floorId) result.add(p);
        }
        return result;
    }

    @NonNull
    @Override
    public List<EntryPoint> getEntryPoints(long floorId) {
        List<EntryPoint> result = new ArrayList<>();
        for (EntryPoint e : FakeStore.entryPoints) {
            if (e.floorId == floorId) result.add(e);
        }
        return result;
    }

    @Nullable
    @Override
    public RefillPoint getPoint(long pointId) {
        for (RefillPoint p : FakeStore.points) {
            if (p.pointId == pointId) return p;
        }
        return null;
    }

    @Nullable
    @Override
    public RefillPoint findByQrCode(@NonNull String qrCode) {
        for (RefillPoint p : FakeStore.points) {
            if (qrCode.equals(p.qrCode)) return p;
        }
        return null;
    }

    @Override
    public void updateStatus(long pointId, @NonNull String status, long confirmedAt) {
        RefillPoint p = getPoint(pointId);
        if (p != null) {
            p.status = status;
            p.lastConfirmedAt = confirmedAt;
        }
    }

    @Override
    public long addPoint(@NonNull RefillPoint point) {
        point.pointId = FakeStore.newId();
        FakeStore.points.add(point);
        return point.pointId;
    }

    @Override
    public void updatePoint(@NonNull RefillPoint point) {
        for (int i = 0; i < FakeStore.points.size(); i++) {
            if (FakeStore.points.get(i).pointId == point.pointId) {
                FakeStore.points.set(i, point);
                return;
            }
        }
    }

    @Override
    public void deletePoint(long pointId) {
        Iterator<RefillPoint> it = FakeStore.points.iterator();
        while (it.hasNext()) {
            if (it.next().pointId == pointId) it.remove();
        }
    }
}
