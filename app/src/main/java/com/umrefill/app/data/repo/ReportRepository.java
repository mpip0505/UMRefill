package com.umrefill.app.data.repo;

import androidx.annotation.NonNull;

import com.umrefill.app.data.entity.Report;

import java.util.List;

/** Community reports (Module 4). Blocking methods: see RefillPointRepository. */
public interface ReportRepository {
    /** FR4.1. Returns the new reportId. */
    long submit(@NonNull Report report);

    /** FR4.3: unresolved reports, newest first. */
    @NonNull List<Report> getOpenReports();

    @NonNull List<Report> getReportsForPoint(long pointId);

    /** FR4.3: mark a report reviewed and set the point's new status. */
    void resolve(long reportId, long adminUserId, @NonNull String newPointStatus);
}
