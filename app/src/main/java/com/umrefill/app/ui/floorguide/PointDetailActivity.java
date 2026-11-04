package com.umrefill.app.ui.floorguide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.umrefill.app.R;
import com.umrefill.app.ui.common.BaseActivity;
import com.umrefill.app.ui.common.Placeholder;
import com.umrefill.app.ui.report.ReportActivity;
import com.umrefill.app.util.Extras;

/** OWNER: Dev B (floor guide and QR). Opened from a pin, the list, or a QR scan. */
public class PointDetailActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_detail);
        setTitle(R.string.title_point);
        long pointId = getIntent().getLongExtra(Extras.POINT_ID, -1);

        View root = findViewById(android.R.id.content);
        Placeholder.show(root, "TODO Dev B\n"
                + "FR3.3: show the point's type, status and last-confirmed date (repo.getPoint(pointId)).\n"
                + "'Report status' button -> ReportActivity (login needed: SessionManager.isLoggedIn()).\n"
                + "'I refilled here' button -> ServiceLocator.impact().logRefill(...) (FR5.1).\n"
                + "Reload in onResume() so a new report shows straight away.");

        Placeholder.shortcut(root, 1, "Report status (temp)", v -> {
            Intent i = new Intent(this, ReportActivity.class);
            i.putExtra(Extras.POINT_ID, pointId);
            startActivity(i);
        });
    }
}
