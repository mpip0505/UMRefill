package com.umrefill.app.ui.admin;

import android.os.Bundle;

import com.umrefill.app.R;
import com.umrefill.app.ui.common.BaseActivity;
import com.umrefill.app.ui.common.Placeholder;

/** OWNER: Dev C (reporting and admin). */
public class ReportQueueActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_queue);
        setTitle(R.string.title_report_queue);
        Placeholder.show(findViewById(android.R.id.content), "TODO Dev C\n"
                + "FR4.3: list open reports, newest first (ServiceLocator.reports().getOpenReports()).\n"
                + "Tap one -> mark the point fixed or out of service (reports().resolve(...)).\n"
                + "FR6.3: show 'likely faulty' points first (FaultFlagger, from the AI lead).");
    }
}
