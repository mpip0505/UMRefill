package com.umrefill.app.ui.report;

import android.os.Bundle;
import android.view.View;

import com.umrefill.app.R;
import com.umrefill.app.ui.common.BaseActivity;
import com.umrefill.app.ui.common.Placeholder;
import com.umrefill.app.util.Extras;

/** OWNER: Dev C (reporting and admin). */
public class ReportActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        setTitle(R.string.title_report);
        long pointId = getIntent().getLongExtra(Extras.POINT_ID, -1);

        Placeholder.show(findViewById(android.R.id.content), "TODO Dev C (point " + pointId + ")\n"
                + "FR4.1: pick a status (working / broken / empty / dirty), optional note and photo.\n"
                + "Submit -> ServiceLocator.reports().submit(report) with SessionManager.currentUser().\n"
                + "FR4.2: then repo.updateStatus(pointId, status, now).\n"
                + "Later: PhotoChecker (FR6.1) and ReportClassifier (FR6.2) hooks from the AI lead.");
    }
}
