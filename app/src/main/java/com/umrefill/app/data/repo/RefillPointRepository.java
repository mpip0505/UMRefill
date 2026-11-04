package com.umrefill.app.data.repo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.umrefill.app.data.entity.EntryPoint;
import com.umrefill.app.data.entity.Faculty;
import com.umrefill.app.data.entity.Floor;
import com.umrefill.app.data.entity.RefillPoint;

import java.util.List;

/**
 * Faculties, floors and refill points (Modules 2, 3 and 5 admin).
 * Screens only talk to this interface, never to Room directly, so the storage can change
 * (fake -> Room -> Supabase) without touching any screen.
 *
 * Methods are blocking. The fake version is instant; once Room is plugged in, call them from
 * {@link com.umrefill.app.util.AppExecutors#io()} and post results back to the main thread.
 */
public interface RefillPointRepository {
    @NonNull List<Faculty> getFaculties();

    @NonNull List<Floor> getFloors(long facultyId);

    @Nullable Floor getFloor(long floorId);

    @NonNull List<RefillPoint> getPoints(long floorId);

    @NonNull List<EntryPoint> getEntryPoints(long floorId);

    @Nullable RefillPoint getPoint(long pointId);

    @Nullable RefillPoint findByQrCode(@NonNull String qrCode);

    /** FR4.2: set the status and the last-confirmed time. */
    void updateStatus(long pointId, @NonNull String status, long confirmedAt);

    // Admin (FR5.3)
    long addPoint(@NonNull RefillPoint point);

    void updatePoint(@NonNull RefillPoint point);

    void deletePoint(long pointId);
}
