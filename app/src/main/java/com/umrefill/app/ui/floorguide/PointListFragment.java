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

/** "List" view of a floor. OWNER: Dev A (browse and list). */
public class PointListFragment extends Fragment {

    public static PointListFragment newInstance(long floorId) {
        PointListFragment f = new PointListFragment();
        Bundle args = new Bundle();
        args.putLong(Extras.FLOOR_ID, floorId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_point_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        long floorId = requireArguments().getLong(Extras.FLOOR_ID);
        Placeholder.show(view, "TODO Dev A\n"
                + "FR2.2: list the floor's points (repo.getPoints(floorId)) with name, status and location note.\n"
                + "FR2.3: filters for 'working only' and type (cold / filtered / normal).\n"
                + "Show status with an icon or word, not colour alone. Refresh in onResume().\n"
                + "Tap a point -> PointDetailActivity with Extras.POINT_ID.");

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
