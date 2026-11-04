package com.umrefill.app.ai;

import androidx.annotation.NonNull;

import com.umrefill.app.data.entity.Report;

import java.util.List;

/**
 * FR6.3: flags a point as "likely faulty" after repeated negative reports in a short window,
 * so the admin report queue can show it first. Owner: data/AI lead.
 */
public final class FaultFlagger {
    private FaultFlagger() {}

    public static boolean isLikelyFaulty(@NonNull List<Report> reportsForPoint, long now) {
        // TODO FR6.3: decide the threshold and time window, implement, and unit-test it.
        return false;
    }
}
