package com.umrefill.app.data;

import com.umrefill.app.data.fake.FakeImpactRepository;
import com.umrefill.app.data.fake.FakeRefillPointRepository;
import com.umrefill.app.data.fake.FakeReportRepository;
import com.umrefill.app.data.fake.FakeUserRepository;
import com.umrefill.app.data.repo.ImpactRepository;
import com.umrefill.app.data.repo.RefillPointRepository;
import com.umrefill.app.data.repo.ReportRepository;
import com.umrefill.app.data.repo.UserRepository;

/**
 * The one place that decides which repository implementation the app uses.
 * Screens call ServiceLocator.refillPoints() etc. and never create repositories themselves.
 * When the Room versions are ready (data lead), only this file changes.
 */
public final class ServiceLocator {
    private static RefillPointRepository refillPoints;
    private static ReportRepository reports;
    private static UserRepository users;
    private static ImpactRepository impact;

    private ServiceLocator() {}

    public static synchronized RefillPointRepository refillPoints() {
        if (refillPoints == null) refillPoints = new FakeRefillPointRepository();
        return refillPoints;
    }

    public static synchronized ReportRepository reports() {
        if (reports == null) reports = new FakeReportRepository(refillPoints());
        return reports;
    }

    public static synchronized UserRepository users() {
        if (users == null) users = new FakeUserRepository();
        return users;
    }

    public static synchronized ImpactRepository impact() {
        if (impact == null) impact = new FakeImpactRepository();
        return impact;
    }
}
