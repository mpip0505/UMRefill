package com.umrefill.app.ui.admin;

import android.os.Bundle;

import com.umrefill.app.R;
import com.umrefill.app.ui.common.BaseActivity;
import com.umrefill.app.ui.common.Placeholder;

/** OWNER: Dev C (reporting and admin). */
public class ManagePointsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_points);
        setTitle(R.string.title_manage_points);
        Placeholder.show(findViewById(android.R.id.content), "TODO Dev C\n"
                + "FR5.3: add, edit or remove refill points (repo.addPoint / updatePoint / deletePoint)\n"
                + "and export a CSV of points and faults.");
    }
}
