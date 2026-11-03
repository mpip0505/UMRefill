package com.umrefill.app.data.fake;

import androidx.annotation.NonNull;

import com.umrefill.app.data.entity.Report;
import com.umrefill.app.data.repo.RefillPointRepository;
import com.umrefill.app.data.repo.ReportRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeReportRepository implements ReportRepository {
    private final RefillPointRepository points;

    public FakeReportRepository(RefillPointRepository points) {
        FakeStore.ensureSeeded();
        this.points = points;
    }

    @Override
    public long submit(@NonNull Report report) {
        report.reportId = FakeStore.newId();
        if (report.createdAt == 0) report.createdAt = System.currentTimeMillis();
        FakeStore.reports.add(report);
        return report.reportId;
    }

    @NonNull
    @Override
    public List<Report> getOpenReports() {
        List<Report> result = new ArrayList<>();
        for (Report r : FakeStore.reports) {
            if (!r.resolved) result.add(r);
        }
        Collections.sort(result, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        return result;
    }

    @NonNull
    @Override
    public List<Report> getReportsForPoint(long pointId) {
        List<Report> result = new ArrayList<>();
        for (Report r : FakeStore.reports) {
            if (r.pointId == pointId) result.add(r);
        }
        Collections.sort(result, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        return result;
    }

    @Override
    public void resolve(long reportId, long adminUserId, @NonNull String newPointStatus) {
        for (Report r : FakeStore.reports) {
            if (r.reportId == reportId) {
                r.resolved = true;
                r.resolvedBy = adminUserId;
                points.updateStatus(r.pointId, newPointStatus, System.currentTimeMillis());
                return;
            }
        }
    }
}
