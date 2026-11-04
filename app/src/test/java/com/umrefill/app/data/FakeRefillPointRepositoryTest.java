package com.umrefill.app.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.umrefill.app.data.entity.Floor;
import com.umrefill.app.data.entity.PointStatus;
import com.umrefill.app.data.entity.RefillPoint;
import com.umrefill.app.data.fake.FakeRefillPointRepository;
import com.umrefill.app.data.repo.RefillPointRepository;

import org.junit.Test;

import java.util.List;

/**
 * Example local unit test (runs on your computer, no emulator): right-click > Run.
 * Copy this pattern for your own module's logic, and name tests after the FR they check.
 */
public class FakeRefillPointRepositoryTest {
    private final RefillPointRepository repo = new FakeRefillPointRepository();

    @Test
    public void sampleDataHasFaculties() {
        assertFalse(repo.getFaculties().isEmpty());
    }

    @Test
    public void floorsAreSortedByLevel() {
        List<Floor> floors = repo.getFloors(1);
        for (int i = 1; i < floors.size(); i++) {
            assertFalse(floors.get(i).level < floors.get(i - 1).level);
        }
    }

    @Test
    public void qrCodeFindsItsPoint() {
        RefillPoint p = repo.findByQrCode("UMR-FSKTM-G-01");
        assertNotNull(p);
        assertNull(repo.findByQrCode("NOT-A-CODE"));
    }

    @Test
    public void updateStatusChangesPoint() {
        repo.updateStatus(1, PointStatus.EMPTY, 123L);
        RefillPoint p = repo.getPoint(1);
        assertEquals(PointStatus.EMPTY, p.status);
        assertEquals(123L, p.lastConfirmedAt);
        repo.updateStatus(1, PointStatus.WORKING, 456L); // put it back for other tests
    }
}
