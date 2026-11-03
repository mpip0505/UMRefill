package com.umrefill.app.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background and main-thread executors. Room refuses to run on the main thread, so once the
 * Room repositories are in, load data like this:
 *
 * <pre>
 * AppExecutors.io().execute(() -> {
 *     List&lt;Faculty&gt; list = ServiceLocator.refillPoints().getFaculties();
 *     AppExecutors.main().execute(() -> adapter.submit(list));
 * });
 * </pre>
 */
public final class AppExecutors {
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Executor MAIN = MAIN_HANDLER::post;

    private AppExecutors() {}

    public static ExecutorService io() {
        return IO;
    }

    public static Executor main() {
        return MAIN;
    }
}
