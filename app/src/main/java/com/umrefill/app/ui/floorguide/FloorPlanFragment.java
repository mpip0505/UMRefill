package com.umrefill.app.ui.floorguide;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.umrefill.app.R;
import com.umrefill.app.data.ServiceLocator;
import com.umrefill.app.data.entity.RefillPoint;
import com.umrefill.app.ui.common.Placeholder;
import com.umrefill.app.util.Extras;

import java.util.List;

/** "Plan" view of a floor: the signature feature. OWNER: Dev B (floor guide and QR). */
public class FloorPlanFragment extends Fragment {

    public static FloorPlanFragment newInstance(long floorId) {
        FloorPlanFragment f = new FloorPlanFragment();
        Bundle args = new Bundle();
        args.putLong(Extras.FLOOR_ID, floorId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_floor_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        long floorId = requireArguments().getLong(Extras.FLOOR_ID);
        Placeholder.show(view, "TODO Dev B\n"
                + "FR3.1: show the floor plan image (Floor.planImage) with a pin per point at xPct / yPct.\n"
                + "FR3.2: let the user pick an entry point (repo.getEntryPoints) and highlight the nearest WORKING point.\n"
                + "Tap a pin -> PointDetailActivity. NFR2: plan + pins render in under 2 s.");

        List<RefillPoint> points = ServiceLocator.refillPoints().getPoints(floorId);
        if (!points.isEmpty()) {
            long pointId = points.get(0).pointId;
            Placeholder.shortcut(view, 1, "Open a point on this floor (temp)", v -> {
                Intent i = new Intent(requireContext(), PointDetailActivity.class);
                i.putExtra(Extras.POINT_ID, pointId);
                startActivity(i);
            });
        }
    }
}
