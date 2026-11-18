package com.umrefill.app.util.pathfinding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents a walkable junction, entrance, or refill station waypoint on a 2D floor plan.
 * Coordinates use normalized percentages (0.0f to 1.0f) for resolution independence.
 */
public class GraphNode {
    @NonNull
    public String id = "";

    public float xPct;
    public float yPct;

    @Nullable
    public String label;

    public GraphNode() {
        // Default constructor required by Gson for JSON deserialization
    }

    public GraphNode(@NonNull String id, float xPct, float yPct, @Nullable String label) {
        this.id = id;
        this.xPct = xPct;
        this.yPct = yPct;
        this.label = label;
    }
}