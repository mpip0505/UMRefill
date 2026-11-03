package com.umrefill.app.ai;

import androidx.annotation.Nullable;

/**
 * FR6.2: suggests a status from the free-text note of a report.
 * Owner: data/AI lead. Add unit tests in app/src/test as you build it.
 */
public final class ReportClassifier {
    private ReportClassifier() {}

    /** Returns one of PointStatus, or null when no suggestion. */
    @Nullable
    public static String suggestStatus(@Nullable String note) {
        // TODO FR6.2: keyword rules (English + BM) or a small TFLite model.
        return null;
    }
}
