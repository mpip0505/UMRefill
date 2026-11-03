package com.umrefill.app.ui.browse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.umrefill.app.R;
import com.umrefill.app.data.ServiceLocator;
import com.umrefill.app.data.entity.Floor;
import com.umrefill.app.ui.common.BaseActivity;
import com.umrefill.app.ui.common.Placeholder;
import com.umrefill.app.ui.floorguide.FloorActivity;
import com.umrefill.app.util.Extras;

import java.util.List;

/** OWNER: Dev A (browse and list). */
public class FloorListActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floor_list);
        setTitle(R.string.title_floors);
        long facultyId = getIntent().getLongExtra(Extras.FACULTY_ID, -1);

        View root = findViewById(android.R.id.content);
        Placeholder.show(root, "TODO Dev A\n"
                + "FR2.1: list this faculty's floors (repo.getFloors(facultyId)), sorted by level.\n"
                + "Tap a floor -> FloorActivity with Extras.FLOOR_ID.\n"
                + "Tip: copy the pattern in BrowseFragment.");

        // TEMP shortcut until FR2.1 is built
        List<Floor> floors = ServiceLocator.refillPoints().getFloors(facultyId);
        if (!floors.isEmpty()) {
            long firstFloorId = floors.get(0).floorId;
            Placeholder.shortcut(root, 1, "Open first floor (temp)", v -> {
                Intent i = new Intent(this, FloorActivity.class);
                i.putExtra(Extras.FLOOR_ID, firstFloorId);
                startActivity(i);
            });
        }
    }
}
