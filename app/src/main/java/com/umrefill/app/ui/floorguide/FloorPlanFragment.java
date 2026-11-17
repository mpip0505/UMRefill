package com.umrefill.app.ui.floorguide;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.umrefill.app.R;
import com.umrefill.app.data.ServiceLocator;
import com.umrefill.app.data.entity.EntryPoint;
import com.umrefill.app.data.entity.Floor;
import com.umrefill.app.data.entity.PointStatus;
import com.umrefill.app.data.entity.RefillPoint;
import com.umrefill.app.data.repo.RefillPointRepository;
import com.umrefill.app.util.AppExecutors;
import com.umrefill.app.util.Extras;
import com.umrefill.app.util.pathfinding.FloorGraph;
import com.umrefill.app.util.pathfinding.Pathfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Interactive 2D Floor Plan Fragment supporting Pinch-to-Zoom, Strict Corridor Snapping,
 * Hybrid JSON + BitmapCorridorExtractor loading, and Cross-Floor Faculty Fallback routing.
 */
public class FloorPlanFragment extends Fragment {

    private long floorId = -1L;
    private DynamicMapView imgFloorPlan;
    private ChipGroup chipGroupFloors;
    private MaterialAutoCompleteTextView dropdownEntryPoint;
    private MaterialCardView cardNearestPoint;
    private TextView txtNearestOverline;
    private TextView txtNearestBadge;
    private TextView txtNearestTitle;
    private TextView txtNearestNote;

    private final List<Floor> siblingFloors = new ArrayList<>();
    private final List<EntryPoint> entryPoints = new ArrayList<>();
    private final List<RefillPoint> refillPoints = new ArrayList<>();
    @Nullable
    private FloorGraph floorGraph;
    private int selectedEntryIndex = 0;
    @Nullable
    private EntryPoint activeEntryPoint;

    // Cross-floor fallback state when all points on the current floor are BROKEN/EMPTY
    @Nullable
    private Floor fallbackWorkingFloor;
    @Nullable
    private RefillPoint fallbackWorkingPoint;

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
        if (getArguments() != null) {
            floorId = getArguments().getLong(Extras.FLOOR_ID, -1L);
        }

        applyEdgeToEdgeInsetsFix(view);

        imgFloorPlan = view.findViewById(R.id.imgFloorPlan);
        chipGroupFloors = view.findViewById(R.id.chipGroupFloors);
        dropdownEntryPoint = view.findViewById(R.id.dropdownEntryPoint);
        cardNearestPoint = view.findViewById(R.id.cardNearestPoint);
        txtNearestOverline = view.findViewById(R.id.txtNearestOverline);
        txtNearestBadge = view.findViewById(R.id.txtNearestBadge);
        txtNearestTitle = view.findViewById(R.id.txtNearestTitle);
        txtNearestNote = view.findViewById(R.id.txtNearestNote);

        // Long-press the top yellow notice banner to toggle between Curated JSON and BitmapCorridorExtractor live
        View cardNotice = view.findViewById(R.id.cardFloorNotice);
        if (cardNotice != null) {
            cardNotice.setOnLongClickListener(v -> {
                Pathfinder.FORCE_BITMAP_EXTRACTOR_FOR_TEST = !Pathfinder.FORCE_BITMAP_EXTRACTOR_FOR_TEST;
                String mode = Pathfinder.FORCE_BITMAP_EXTRACTOR_FOR_TEST
                        ? "Auto BitmapCorridorExtractor Mode (Vision Grid)"
                        : "Curated JSON Graph Mode (Assets)";
                android.widget.Toast.makeText(requireContext(), mode, android.widget.Toast.LENGTH_SHORT).show();
                loadFloorAndComputeRoute();
                return true;
            });
        }

        imgFloorPlan.setOnPointPinClickListener(this::openPointDetail);

        // Snap user tap/drag directly onto the nearest walkable corridor centerline
        imgFloorPlan.setOnCustomEntryMoveListener((xPct, yPct) -> {
            if (!isAdded()) return;
            EntryPoint rawTap = new EntryPoint(
                    -999L, floorId, getString(R.string.custom_map_position), xPct, yPct);
            activeEntryPoint = Pathfinder.snapEntryToCorridor(rawTap, floorGraph);
            dropdownEntryPoint.setText(getString(R.string.custom_map_position), false);
            recalculateAndRenderPath();
        });

        dropdownEntryPoint.setOnItemClickListener((parent, itemView, position, id) -> {
            selectedEntryIndex = position;
            if (position >= 0 && position < entryPoints.size()) {
                activeEntryPoint = Pathfinder.snapEntryToCorridor(entryPoints.get(position), floorGraph);
                recalculateAndRenderPath();
            }
        });
    }

    private void applyEdgeToEdgeInsetsFix(@NonNull View fragmentRoot) {
        View activityContent = requireActivity().findViewById(android.R.id.content);
        View target = activityContent != null ? activityContent : fragmentRoot;
        ViewCompat.setOnApplyWindowInsetsListener(target, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFloorAndComputeRoute();
    }

    private void loadFloorAndComputeRoute() {
        AppExecutors.io().execute(() -> {
            RefillPointRepository repo = ServiceLocator.refillPoints();
            Floor floor = repo.getFloor(floorId);
            List<Floor> facultyFloors = (floor != null)
                    ? repo.getFloors(floor.facultyId) : Collections.emptyList();
            List<EntryPoint> loadedEntries = repo.getEntryPoints(floorId);
            List<RefillPoint> loadedPoints = repo.getPoints(floorId);

            // Check if current floor has at least one WORKING station; if not, find nearest working floor in faculty
            boolean hasWorkingOnCurrent = false;
            for (RefillPoint p : loadedPoints) {
                if (PointStatus.WORKING.equals(p.status)) {
                    hasWorkingOnCurrent = true;
                    break;
                }
            }

            Floor foundFallbackFloor = null;
            RefillPoint foundFallbackPoint = null;
            if (!hasWorkingOnCurrent && floor != null) {
                int bestLevelDiff = Integer.MAX_VALUE;
                for (Floor candidateFloor : facultyFloors) {
                    if (candidateFloor.floorId == floorId) continue;
                    List<RefillPoint> otherPoints = repo.getPoints(candidateFloor.floorId);
                    for (RefillPoint op : otherPoints) {
                        if (PointStatus.WORKING.equals(op.status)) {
                            int diff = Math.abs(candidateFloor.level - floor.level);
                            if (diff < bestLevelDiff) {
                                bestLevelDiff = diff;
                                foundFallbackFloor = candidateFloor;
                                foundFallbackPoint = op;
                            }
                            break;
                        }
                    }
                }
            }

            FloorGraph loadedGraph = null;
            int drawableResId = 0;
            if (floor != null && floor.planImage != null && isAdded()) {
                drawableResId = resolveDrawableId(floor.planImage);
                Bitmap fallbackBmp = null;
                if (drawableResId != 0 && Pathfinder.FORCE_BITMAP_EXTRACTOR_FOR_TEST) {
                    fallbackBmp = BitmapFactory.decodeResource(requireContext().getResources(), drawableResId);
                }
                loadedGraph = Pathfinder.loadGraphOrExtract(requireContext(), floor.planImage, fallbackBmp);
                if (loadedGraph == null && drawableResId != 0) {
                    fallbackBmp = BitmapFactory.decodeResource(requireContext().getResources(), drawableResId);
                    loadedGraph = Pathfinder.loadGraphOrExtract(requireContext(), floor.planImage, fallbackBmp);
                }
            }

            final Floor finalFloor = floor;
            final FloorGraph finalGraph = loadedGraph;
            final int finalDrawableResId = drawableResId;
            final Floor finalFallbackFloor = foundFallbackFloor;
            final RefillPoint finalFallbackPoint = foundFallbackPoint;

            AppExecutors.main().execute(() -> {
                if (!isAdded()) return;
                if (finalFloor != null) {
                    requireActivity().setTitle(finalFloor.label);
                }
                this.floorGraph = finalGraph;
                this.siblingFloors.clear();
                this.siblingFloors.addAll(facultyFloors);
                this.entryPoints.clear();
                this.entryPoints.addAll(loadedEntries);
                this.refillPoints.clear();
                this.refillPoints.addAll(loadedPoints);
                this.fallbackWorkingFloor = finalFallbackFloor;
                this.fallbackWorkingPoint = finalFallbackPoint;

                if (finalDrawableResId != 0) {
                    imgFloorPlan.setImageResource(finalDrawableResId);
                } else {
                    imgFloorPlan.setImageDrawable(null);
                }

                bindFloorSwitcherChips();
                bindEntryDropdown();
                recalculateAndRenderPath();
            });
        });
    }

    private int resolveDrawableId(@NonNull String planImage) {
        String pkg = requireContext().getPackageName();
        int id = requireContext().getResources().getIdentifier(planImage, "drawable", pkg);
        if (id != 0) return id;

        switch (planImage) {
            case "fbl_anggerik_g":
                return requireContext().getResources().getIdentifier("map_fbl_anggerik_ground", "drawable", pkg);
            case "fbl_anggerik_f1":
                return requireContext().getResources().getIdentifier("map_fbl_anggerik_floor1", "drawable", pkg);
            case "fbl_bakawali_g":
                return requireContext().getResources().getIdentifier("map_fbl_bakawali_ground", "drawable", pkg);
            case "fbl_bakawali_f1":
                return requireContext().getResources().getIdentifier("map_fbl_bakawali_floor1", "drawable", pkg);
            default:
                return 0;
        }
    }

    private void bindFloorSwitcherChips() {
        chipGroupFloors.removeAllViews();
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        ColorStateList bgColors = new ColorStateList(states, new int[]{
                Color.parseColor("#D6E4F5"),
                Color.parseColor("#FFFFFF")
        });
        ColorStateList textColors = new ColorStateList(states, new int[]{
                Color.parseColor("#0B2A47"),
                Color.parseColor("#4A5361")
        });

        for (Floor f : siblingFloors) {
            Chip chip = new Chip(requireContext());
            chip.setText(f.label);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChipBackgroundColor(bgColors);
            chip.setTextColor(textColors);
            chip.setChipStrokeWidth(2f);
            chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#C4CBD4")));
            chip.setEnsureMinTouchTargetSize(true);
            chip.setChecked(f.floorId == floorId);

            chip.setOnClickListener(v -> switchToFloor(f.floorId));
            chipGroupFloors.addView(chip);
        }
    }

    private void switchToFloor(long newFloorId) {
        if (newFloorId == floorId) return;
        floorId = newFloorId;
        selectedEntryIndex = 0;
        activeEntryPoint = null;
        imgFloorPlan.resetZoom();
        if (getArguments() != null) {
            getArguments().putLong(Extras.FLOOR_ID, floorId);
        }
        loadFloorAndComputeRoute();
    }

    private void bindEntryDropdown() {
        List<String> names = new ArrayList<>();
        for (EntryPoint ep : entryPoints) {
            names.add(ep.name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, names);
        dropdownEntryPoint.setAdapter(adapter);

        if (!entryPoints.isEmpty()) {
            if (selectedEntryIndex >= entryPoints.size()) {
                selectedEntryIndex = 0;
            }
            if (activeEntryPoint == null || activeEntryPoint.entryId != -999L) {
                activeEntryPoint = Pathfinder.snapEntryToCorridor(
                        entryPoints.get(selectedEntryIndex), floorGraph);
                dropdownEntryPoint.setText(activeEntryPoint.name, false);
            }
        }
    }

    private void recalculateAndRenderPath() {
        if (activeEntryPoint == null && !entryPoints.isEmpty()) {
            activeEntryPoint = Pathfinder.snapEntryToCorridor(entryPoints.get(0), floorGraph);
        }
        if (activeEntryPoint == null) {
            imgFloorPlan.setFloorData(refillPoints, null, Collections.emptyList(), -1L);
            txtNearestOverline.setText(R.string.nearest_overline_none);
            txtNearestBadge.setVisibility(View.GONE);
            txtNearestTitle.setText(R.string.no_working_points);
            txtNearestNote.setText("");
            cardNearestPoint.setOnClickListener(null);
            return;
        }

        Pathfinder.RouteResult route = Pathfinder.findRouteToNearestWorking(
                activeEntryPoint, refillPoints, floorGraph);

        if (route.targetPoint != null) {
            RefillPoint nearest = route.targetPoint;
            imgFloorPlan.setFloorData(refillPoints, activeEntryPoint, route.path, nearest.pointId);
            txtNearestOverline.setText(getString(
                    R.string.nearest_overline_format, route.estimatedMeters));
            txtNearestBadge.setVisibility(View.VISIBLE);
            txtNearestBadge.setBackgroundColor(Color.parseColor("#D8F1E8"));
            txtNearestBadge.setTextColor(Color.parseColor("#00573F"));
            txtNearestBadge.setText(R.string.badge_available);
            txtNearestTitle.setText(nearest.name);
            txtNearestNote.setText(getString(
                    R.string.nearest_direction_format,
                    nearest.type,
                    route.turnCount,
                    nearest.locationNote));
            cardNearestPoint.setOnClickListener(v -> openPointDetail(nearest));
        } else if (fallbackWorkingFloor != null && fallbackWorkingPoint != null) {
            // Cross-Floor Faculty Fallback: route user to nearest Stairs/Lift and offer 1-tap floor switch
            Pathfinder.RouteResult exitRoute = Pathfinder.findRouteToNearestExitStairs(
                    activeEntryPoint, entryPoints, floorGraph);
            imgFloorPlan.setFloorData(refillPoints, activeEntryPoint, exitRoute.path, -1L);

            String stairsName = (exitRoute.targetExitStairs != null)
                    ? exitRoute.targetExitStairs.name : "Stairs / Lift";
            txtNearestOverline.setText(getString(
                    R.string.cross_floor_overline_format, fallbackWorkingFloor.label));
            txtNearestBadge.setVisibility(View.VISIBLE);
            txtNearestBadge.setBackgroundColor(Color.parseColor("#FCEFD2"));
            txtNearestBadge.setTextColor(Color.parseColor("#6B4500"));
            txtNearestBadge.setText(R.string.badge_switch_floor);
            txtNearestTitle.setText(getString(
                    R.string.cross_floor_title_format, stairsName, fallbackWorkingPoint.name));
            txtNearestNote.setText(getString(
                    R.string.cross_floor_note_format,
                    fallbackWorkingPoint.type,
                    fallbackWorkingFloor.label));
            final long targetFloorId = fallbackWorkingFloor.floorId;
            cardNearestPoint.setOnClickListener(v -> switchToFloor(targetFloorId));
        } else {
            imgFloorPlan.setFloorData(refillPoints, activeEntryPoint, Collections.emptyList(), -1L);
            txtNearestOverline.setText(R.string.nearest_overline_none);
            txtNearestBadge.setVisibility(View.GONE);
            txtNearestTitle.setText(R.string.no_working_points);
            txtNearestNote.setText("");
            cardNearestPoint.setOnClickListener(null);
        }
    }

    private void openPointDetail(@NonNull RefillPoint point) {
        Intent intent = new Intent(requireContext(), PointDetailActivity.class);
        intent.putExtra(Extras.POINT_ID, point.pointId);
        startActivity(intent);
    }
}