package com.umrefill.app.util.pathfinding;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Root container matching the JSON schema in assets/maps/<faculty>/<floor>_graph.json.
 */
public class FloorGraph {
    @NonNull
    public List<GraphNode> nodes = new ArrayList<>();

    @NonNull
    public List<GraphEdge> edges = new ArrayList<>();

    public FloorGraph() {}
}