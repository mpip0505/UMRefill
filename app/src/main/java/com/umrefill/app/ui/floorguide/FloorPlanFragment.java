package com.umrefill.app.ui.floorguide;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.umrefill.app.BuildConfig;
import com.umrefill.app.R;
import com.umrefill.app.data.ServiceLocator;
import com.umrefill.app.data.entity.EntryPoint;
import com.umrefill.app.data.entity.Floor;
import com.umrefill.app.data.entity.PointStatus;
import com.umrefill.app.data.entity.PointType;
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

        applyFragmentEdgeToEdgeInsets(view);

        imgFloorPlan = view.findViewById(R.id.imgFloorPlan);
        chipGroupFloors = view.findViewById(R.id.chipGroupFloors);
        dropdownEntryPoint = view.findViewById(R.id.dropdownEntryPoint);
        cardNearestPoint = view.findViewById(R.id.cardNearestPoint);
        txtNearestOverline = view.findViewById(R.id.txtNearestOverline);
        txtNearestBadge = view.findViewById(R.id.txtNearestBadge);
        txtNearestTitle = view.findViewById(R.id.txtNearestTitle);
        txtNearestNote = view.findViewById(R.id.txtNearestNote);

        // In debug builds only, long-pressing the notice banner toggles between Curated JSON and BitmapCorridorExtractor
        View cardNotice = view.findViewById(R.id.cardFloorNotice);
        if (cardNotice != null && BuildConfig.DEBUG) {
            cardNotice.setOnLongClickListener(v -> {
                Pathfinder.FORCE_BITMAP_EXTRACTOR_FOR_TEST = !Pathfinder.FORCE_BITMAP_EXTRACTOR_FOR_TEST;
                int modeRes = Pathfinder.FORCE_BITMAP_EXTRACTOR_FOR_TEST
                        ? R.string.debug_mode_bitmap_extractor
                        : R.string.debug_mode_curated_json;
                Toast.makeText(requireContext(), getString(modeRes), Toast.LENGTH_SHORT).show();
                loadFloorAndComputeRoute();
                return true;
            });
        }

        imgFloorPlan.setOnPointPinClickListener(this::openPointDetail);

        // Snap tap or drag coordinates directly onto the nearest walkable corridor centerline
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

    /**
     * Applies bottom system-bar inset padding at the fragment level only so activity containers
     * are not double-padded.
     */
    private void applyFragmentEdgeToEdgeInsets(@NonNull View fragmentRoot) {
        final int baseLeft = fragmentRoot.getPaddingLeft();
        final int baseTop = fragmentRoot.getPaddingTop();
        final int baseRight = fragmentRoot.getPaddingRight();
        final int baseBottom = fragmentRoot.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(fragmentRoot, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(baseLeft, baseTop, baseRight, baseBottom + bars.bottom);
            return windowInsets;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFloorAndComputeRoute();
    }

    private void loadFloorAndComputeRoute() {
        if (!isAdded()) return;
        final Context appContext = requireContext().getApplicationContext();
        final long requestedFloorId = this.floorId;

        AppExecutors.io().execute(() -> {
            RefillPointRepository repo = ServiceLocator.refillPoints();
            Floor floor = repo.getFloor(requestedFloorId);
            List<Floor> facultyFloors = (floor != null)
                    ? repo.getFloors(floor.facultyId) : Collections.emptyList();
            List<EntryPoint> loadedEntries = repo.getEntryPoints(requestedFloorId);
            List<RefillPoint> loadedPoints = repo.getPoints(requestedFloorId);

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
                String currentBuilding = extractBuildingPrefix(floor.label);
                int bestScore = Integer.MAX_VALUE;
                for (Floor candidateFloor : facultyFloors) {
                    if (candidateFloor.floorId == requestedFloorId) continue;
                    List<RefillPoint> otherPoints = repo.getPoints(candidateFloor.floorId);
                    for (RefillPoint op : otherPoints) {
                        if (PointStatus.WORKING.equals(op.status)) {
                            boolean sameBuilding = currentBuilding.equals(extractBuildingPrefix(candidateFloor.label));
                            int buildingPenalty = sameBuilding ? 0 : 100;
                            int score = buildingPenalty + Math.abs(candidateFloor.level - floor.level);
                            if (score < bestScore) {
                                bestScore = score;
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
            if (floor != null && floor.planImage != null) {
                drawableResId = resolveDrawableId(appContext, floor.planImage);
                Bitmap fallbackBmp = null;
                if (drawableResId != 0 && Pathfinder.FORCE_BITMAP_EXTRACTOR_FOR_TEST) {
                    fallbackBmp = BitmapFactory.decodeResource(appContext.getResources(), drawableResId);
                }
                loadedGraph = Pathfinder.loadGraphOrExtract(appContext, floor.planImage, fallbackBmp);
                if (loadedGraph == null && drawableResId != 0) {
                    fallbackBmp = BitmapFactory.decodeResource(appContext.getResources(), drawableResId);
                    loadedGraph = Pathfinder.loadGraphOrExtract(appContext, floor.planImage, fallbackBmp);
                }
            }

            final Floor finalFloor = floor;
            final FloorGraph finalGraph = loadedGraph;
            final int finalDrawableResId = drawableResId;
            final Floor finalFallbackFloor = foundFallbackFloor;
            final RefillPoint finalFallbackPoint = foundFallbackPoint;

            AppExecutors.main().execute(() -> {
                // Ignore stale result if the fragment detached or the user switched to another floor mid-load
                if (!isAdded() || this.floorId != requestedFloorId) return;
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

    @NonNull
    private static String extractBuildingPrefix(@NonNull String label) {
        int dotIdx = label.indexOf('·');
        if (dotIdx > 0) {
            return label.substring(0, dotIdx).trim();
        }
        int dashIdx = label.indexOf('-');
        if (dashIdx > 0) {
            return label.substring(0, dashIdx).trim();
        }
        return "";
    }

    private int resolveDrawableId(@NonNull Context context, @NonNull String planImage) {
        return context.getResources().getIdentifier(planImage, "drawable", context.getPackageName());
    }

    private void bindFloorSwitcherChips() {
        Context context = requireContext();
        chipGroupFloors.removeAllViews();
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        ColorStateList bgColors = new ColorStateList(states, new int[]{
                ContextCompat.getColor(context, R.color.umr_primary_container),
                ContextCompat.getColor(context, R.color.umr_surface_white)
        });
        ColorStateList textColors = new ColorStateList(states, new int[]{
                ContextCompat.getColor(context, R.color.umr_on_primary_container),
                ContextCompat.getColor(context, R.color.umr_on_surface_variant)
        });
        ColorStateList strokeColor = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.umr_outline));

        for (Floor f : siblingFloors) {
            Chip chip = new Chip(context);
            chip.setText(f.label);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChipBackgroundColor(bgColors);
            chip.setTextColor(textColors);
            chip.setChipStrokeWidth(2f);
            chip.setChipStrokeColor(strokeColor);
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
        Context context = requireContext();
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
            txtNearestBadge.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.umr_status_available_bg));
            txtNearestBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.umr_status_available_text));
            txtNearestBadge.setText(R.string.badge_available);
            txtNearestTitle.setText(nearest.name);
            txtNearestNote.setText(getString(
                    R.string.nearest_direction_format,
                    formatPointTypeLabel(nearest.type),
                    route.turnCount,
                    nearest.locationNote));
            cardNearestPoint.setOnClickListener(v -> openPointDetail(nearest));
        } else if (route.hasUnreachableWorkingPoints) {
            imgFloorPlan.setFloorData(refillPoints, activeEntryPoint, Collections.emptyList(), -1L);
            txtNearestOverline.setText(R.string.nearest_overline_unreachable);
            txtNearestBadge.setVisibility(View.GONE);
            txtNearestTitle.setText(R.string.route_unreachable_points);
            txtNearestNote.setText("");
            cardNearestPoint.setOnClickListener(null);
        } else if (fallbackWorkingFloor != null && fallbackWorkingPoint != null) {
            // Cross-Floor Faculty Fallback: route user to nearest Stairs/Lift and offer 1-tap floor switch
            Pathfinder.RouteResult exitRoute = Pathfinder.findRouteToNearestExitStairs(
                    activeEntryPoint, entryPoints, floorGraph);
            imgFloorPlan.setFloorData(refillPoints, activeEntryPoint, exitRoute.path, -1L);

            String stairsName = (exitRoute.targetExitStairs != null)
                    ? exitRoute.targetExitStairs.name : getString(R.string.default_stairs_lift);
            txtNearestOverline.setText(getString(
                    R.string.cross_floor_overline_format, fallbackWorkingFloor.label));
            txtNearestBadge.setVisibility(View.VISIBLE);
            txtNearestBadge.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.umr_status_low_bg));
            txtNearestBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.umr_status_low_text));
            txtNearestBadge.setText(R.string.badge_switch_floor);
            txtNearestTitle.setText(getString(
                    R.string.cross_floor_title_format, stairsName, fallbackWorkingPoint.name));
            txtNearestNote.setText(getString(
                    R.string.cross_floor_note_format,
                    formatPointTypeLabel(fallbackWorkingPoint.type),
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

    @NonNull
    private String formatPointTypeLabel(@NonNull String rawType) {
        switch (rawType) {
            case PointType.COLD:
                return getString(R.string.filter_type_cold);
            case PointType.FILTERED:
                return getString(R.string.filter_type_filtered);
            case PointType.NORMAL:
            default:
                return getString(R.string.filter_type_normal);
        }
    }

    private void openPointDetail(@NonNull RefillPoint point) {
        Intent intent = new Intent(requireContext(), PointDetailActivity.class);
        intent.putExtra(Extras.POINT_ID, point.pointId);
        startActivity(intent);
    }
}