package com.umrefill.app.data.repo;

import androidx.annotation.NonNull;

import java.util.Map;

/** Refill counters (Module 5). Blocking methods: see RefillPointRepository. */
public interface ImpactRepository {
    void logRefill(long userId, long pointId);

    /** FR5.1: refills logged by this user (= bottles avoided). */
    int getRefillCount(long userId);

    /** FR5.2: faculty name -> refills logged at that faculty's points. */
    @NonNull Map<String, Integer> getRefillTotalsByFaculty();
}
