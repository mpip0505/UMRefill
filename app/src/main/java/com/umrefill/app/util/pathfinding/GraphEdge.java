package com.umrefill.app.util.pathfinding;

import androidx.annotation.NonNull;

/**
 * Represents a bidirectional walkable hallway segment between two GraphNodes.
 */
public class GraphEdge {
    @NonNull
    public String from = "";

    @NonNull
    public String to = "";

    public float weight;

    public GraphEdge() {
        // Default constructor required by Gson
    }

    public GraphEdge(@NonNull String from, @NonNull String to, float weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }
}