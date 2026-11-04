package com.umrefill.app.ui.floorguide;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.umrefill.app.R;
import com.umrefill.app.data.ServiceLocator;
import com.umrefill.app.data.entity.Floor;
import com.umrefill.app.ui.common.BaseActivity;
import com.umrefill.app.util.Extras;

/**
 * Shell for one floor: a Plan / List toggle. The two views are separate fragments so Dev B
 * (FloorPlanFragment) and Dev A (PointListFragment) never edit the same file.
 */
public class FloorActivity extends BaseActivity {
    private long floorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floor);

        floorId = getIntent().getLongExtra(Extras.FLOOR_ID, -1);
        Floor floor = ServiceLocator.refillPoints().getFloor(floorId);
        if (floor != null) setTitle(floor.label);

        MaterialButtonToggleGroup toggle = findViewById(R.id.toggleView);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) show(checkedId == R.id.btnPlan);
        });
        if (savedInstanceState == null) show(true);
    }

    private void show(boolean plan) {
        Fragment f = plan ? FloorPlanFragment.newInstance(floorId) : PointListFragment.newInstance(floorId);
        getSupportFragmentManager().beginTransaction().replace(R.id.floorContent, f).commit();
    }
}
