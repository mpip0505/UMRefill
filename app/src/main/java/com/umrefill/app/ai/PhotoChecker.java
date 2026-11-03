package com.umrefill.app.ai;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

/**
 * FR6.1: checks whether a report photo looks like a water fountain or tap.
 * Owner: data/AI lead. Accuracy test on 30-50 UM photos: tester.
 *
 * TODO: add ML Kit Image Labeling (com.google.mlkit:image-labeling), run it on-device,
 * and set looksLikeFountain when a label such as "Tap", "Sink" or "Water" scores above a threshold.
 * The user can always submit anyway: this only warns, it never blocks.
 */
public class PhotoChecker {

    public static class Result {
        public final boolean looksLikeFountain;
        @NonNull public final String topLabel;
        public final float confidence;

        public Result(boolean looksLikeFountain, @NonNull String topLabel, float confidence) {
            this.looksLikeFountain = looksLikeFountain;
            this.topLabel = topLabel;
            this.confidence = confidence;
        }
    }

    @NonNull
    public Result check(@NonNull Context context, @NonNull Uri photo) {
        // Placeholder until ML Kit is added: treat every photo as fine.
        return new Result(true, "unchecked", 0f);
    }
}
