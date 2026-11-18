package com.umrefill.app.util.pathfinding;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.umrefill.app.data.entity.EntryPoint;
import com.umrefill.app.data.entity.PointStatus;
import com.umrefill.app.data.entity.RefillPoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Dynamic corridor pathfinding engine using Strict Corridor-Edge Snapping + Dijkstra's Algorithm,
 * with hybrid BitmapCorridorExtractor fallback and cross-floor stair routing (FR3.2).
 */
public final class Pathfinder {

    /** Debug flag to force BitmapCorridorExtractor instead of curated JSON files during testing. */
    public static boolean FORCE_BITMAP_EXTRACTOR_FOR_TEST = false;

    private static final float APPROX_FLOOR_SPAN_METERS = 85.0f;

    public static final class RouteResult {
        @Nullable
        public final RefillPoint targetPoint;
        @Nullable
        public final EntryPoint targetExitStairs;
        @NonNull
        public final List<GraphNode> path;
        public final float totalDistancePct;
        public final int estimatedMeters;
        public final int turnCount;
        public final boolean hasUnreachableWorkingPoints;

        public RouteResult(@Nullable RefillPoint targetPoint,
                           @Nullable EntryPoint targetExitStairs,
                           @NonNull List<GraphNode> path,
                           float totalDistancePct,
                           int estimatedMeters,
                           int turnCount) {
            this(targetPoint, targetExitStairs, path, totalDistancePct, estimatedMeters, turnCount, false);
        }

        public RouteResult(@Nullable RefillPoint targetPoint,
                           @Nullable EntryPoint targetExitStairs,
                           @NonNull List<GraphNode> path,
                           float totalDistancePct,
                           int estimatedMeters,
                           int turnCount,
                           boolean hasUnreachableWorkingPoints) {
            this.targetPoint = targetPoint;
            this.targetExitStairs = targetExitStairs;
            this.path = path;
            this.totalDistancePct = totalDistancePct;
            this.estimatedMeters = estimatedMeters;
            this.turnCount = turnCount;
            this.hasUnreachableWorkingPoints = hasUnreachableWorkingPoints;
        }
    }

    private static final class NodeRecord {
        @NonNull
        final String nodeId;
        final float distance;

        NodeRecord(@NonNull String nodeId, float distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }

    private Pathfinder() {}

    @Nullable
    @SuppressWarnings("SpellCheckingInspection")
    public static String resolveAssetPath(@Nullable String planImage) {
        if (planImage == null || planImage.trim().isEmpty()) {
            return null;
        }
        String clean = planImage.trim();
        switch (clean) {
            case "map_fbl_anggerik_ground":
            case "fbl_anggerik_g":
                return "maps/fbl/fbl_anggerik_g_graph.json";
            case "map_fbl_anggerik_floor1":
            case "fbl_anggerik_f1":
                return "maps/fbl/fbl_anggerik_f1_graph.json";
            case "map_fbl_bakawali_ground":
            case "fbl_bakawali_g":
                return "maps/fbl/fbl_bakawali_g_graph.json";
            case "map_fbl_bakawali_floor1":
            case "fbl_bakawali_f1":
                return "maps/fbl/fbl_bakawali_f1_graph.json";
            default:
                break;
        }
        if (clean.startsWith("fbl_")) {
            return "maps/fbl/" + clean + "_graph.json";
        } else if (clean.startsWith("fsktm_")) {
            return "maps/fsktm/" + clean + "_graph.json";
        }
        return "maps/" + clean + "_graph.json";
    }

    /**
     * Hybrid Graph Loader: loads curated JSON graph from assets/maps/ first (<1ms),
     * or automatically extracts the corridor graph from the floor plan Bitmap if JSON is missing.
     */
    @Nullable
    public static FloorGraph loadGraphOrExtract(
            @NonNull Context context,
            @Nullable String planImage,
            @Nullable Bitmap fallbackBitmap) {
        if (!FORCE_BITMAP_EXTRACTOR_FOR_TEST) {
            FloorGraph jsonGraph = loadGraphFromAssets(context, planImage);
            if (jsonGraph != null && !jsonGraph.nodes.isEmpty()) {
                return jsonGraph;
            }
        }
        if (fallbackBitmap != null) {
            return BitmapCorridorExtractor.extractFromBitmap(fallbackBitmap);
        }
        return null;
    }

    @Nullable
    public static FloorGraph loadGraphFromAssets(@NonNull Context context, @Nullable String planImage) {
        String assetPath = resolveAssetPath(planImage);
        if (assetPath == null) return null;
        try (InputStream is = context.getAssets().open(assetPath);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, FloorGraph.class);
        } catch (IOException e) {
            return null;
        }
    }

    public static float euclideanDistance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt((dx * dx) + (dy * dy));
    }

    /**
     * Snaps any arbitrary (xPct, yPct) tap coordinate directly onto the nearest walkable corridor edge
     * so the current-position marker never sits inside a wall, room, or empty margin.
     */
    @NonNull
    public static EntryPoint snapEntryToCorridor(
            @NonNull EntryPoint rawEntry,
            @Nullable FloorGraph graph) {
        if (graph == null || graph.nodes.isEmpty()) {
            return rawEntry;
        }
        FloorGraph temp = cloneGraph(graph);
        GraphNode projected = insertProjectedNode(temp, "__snap__", rawEntry.xPct, rawEntry.yPct);
        return new EntryPoint(rawEntry.entryId, rawEntry.floorId, rawEntry.name, projected.xPct, projected.yPct);
    }

    /**
     * Finds the shortest strictly-in-corridor path to the nearest WORKING RefillPoint on this floor.
     */
    @NonNull
    public static RouteResult findRouteToNearestWorking(
            @NonNull EntryPoint startEntry,
            @NonNull List<RefillPoint> floorPoints,
            @Nullable FloorGraph baseGraph) {

        List<RefillPoint> workingPoints = new ArrayList<>();
        for (RefillPoint p : floorPoints) {
            if (PointStatus.WORKING.equals(p.status)) {
                workingPoints.add(p);
            }
        }

        if (workingPoints.isEmpty()) {
            return new RouteResult(null, null, Collections.emptyList(), Float.MAX_VALUE, 0, 0, false);
        }

        if (baseGraph == null || baseGraph.nodes.isEmpty()) {
            RefillPoint bestPoint = null;
            float bestDist = Float.MAX_VALUE;
            for (RefillPoint candidate : workingPoints) {
                float d = euclideanDistance(startEntry.xPct, startEntry.yPct, candidate.xPct, candidate.yPct);
                if (d < bestDist) {
                    bestDist = d;
                    bestPoint = candidate;
                }
            }
            List<GraphNode> directPath = new ArrayList<>();
            if (bestPoint != null) {
                directPath.add(new GraphNode("start", startEntry.xPct, startEntry.yPct, startEntry.name));
                directPath.add(new GraphNode("end", bestPoint.xPct, bestPoint.yPct, bestPoint.name));
            }
            int meters = Math.max(1, Math.round(bestDist * APPROX_FLOOR_SPAN_METERS));
            return new RouteResult(bestPoint, null, directPath, bestDist, meters, 0, false);
        }

        RefillPoint bestTarget = null;
        List<GraphNode> bestPath = Collections.emptyList();
        float bestTotalCost = Float.MAX_VALUE;

        for (RefillPoint candidate : workingPoints) {
            FloorGraph workingGraph = cloneGraph(baseGraph);
            GraphNode startProjected = insertProjectedNode(
                    workingGraph, "__virtual_start__", startEntry.xPct, startEntry.yPct);
            GraphNode goalProjected = insertProjectedNode(
                    workingGraph, "__virtual_goal__", candidate.xPct, candidate.yPct);

            // Strictly use corridorPath only to prevent diagonal shortcuts across rooms
            List<GraphNode> corridorPath = computeShortestPath(
                    workingGraph, startProjected.id, goalProjected.id);
            if (corridorPath.isEmpty()) continue;

            List<GraphNode> simplified = simplifyCollinearNodes(corridorPath);
            float pathCost = calculatePathCost(simplified);

            if (pathCost < bestTotalCost) {
                bestTotalCost = pathCost;
                bestTarget = candidate;
                bestPath = simplified;
            }
        }

        if (bestTarget == null) {
            return new RouteResult(null, null, Collections.emptyList(), Float.MAX_VALUE, 0, 0, true);
        }

        int meters = Math.max(1, Math.round(bestTotalCost * APPROX_FLOOR_SPAN_METERS));
        int turns = countSignificantTurns(bestPath);
        return new RouteResult(bestTarget, null, bestPath, bestTotalCost, meters, turns, false);
    }

    /**
     * Cross-Floor Fallback: When all stations on the current floor are BROKEN/EMPTY,
     * routes the user along the corridors to the nearest staircase/lift (EntryPoint)
     * so they can transition to the nearest working floor in the same faculty.
     */
    @NonNull
    public static RouteResult findRouteToNearestExitStairs(
            @NonNull EntryPoint currentPosition,
            @NonNull List<EntryPoint> floorExits,
            @Nullable FloorGraph baseGraph) {

        if (floorExits.isEmpty() || baseGraph == null || baseGraph.nodes.isEmpty()) {
            return new RouteResult(null, null, Collections.emptyList(), Float.MAX_VALUE, 0, 0);
        }

        List<EntryPoint> verticalConnectors = new ArrayList<>();
        for (EntryPoint ep : floorExits) {
            if (isVerticalTransitionExit(ep)) {
                verticalConnectors.add(ep);
            }
        }
        List<EntryPoint> candidates = verticalConnectors.isEmpty() ? floorExits : verticalConnectors;

        EntryPoint bestExit = null;
        List<GraphNode> bestPath = Collections.emptyList();
        float bestCost = Float.MAX_VALUE;

        for (EntryPoint exit : candidates) {
            FloorGraph workingGraph = cloneGraph(baseGraph);
            GraphNode startProjected = insertProjectedNode(
                    workingGraph, "__virtual_start__", currentPosition.xPct, currentPosition.yPct);
            GraphNode goalProjected = insertProjectedNode(
                    workingGraph, "__virtual_exit__", exit.xPct, exit.yPct);

            List<GraphNode> corridorPath = computeShortestPath(
                    workingGraph, startProjected.id, goalProjected.id);
            if (corridorPath.isEmpty()) continue;

            List<GraphNode> simplified = simplifyCollinearNodes(corridorPath);
            float cost = calculatePathCost(simplified);
            if (cost < bestCost) {
                bestCost = cost;
                bestExit = exit;
                bestPath = simplified;
            }
        }

        int meters = Math.max(1, Math.round(bestCost * APPROX_FLOOR_SPAN_METERS));
        int turns = countSignificantTurns(bestPath);
        return new RouteResult(null, bestExit, bestPath, bestCost, meters, turns);
    }

    /**
     * Filters EntryPoints for vertical floor-to-floor connectors (stairs, lifts, bridges)
     * while excluding ground-only main entrances and flat corridor junctions.
     */
    public static boolean isVerticalTransitionExit(@NonNull EntryPoint entryPoint) {
        String lower = entryPoint.name.toLowerCase(Locale.ROOT);
        if (lower.contains("main entrance")) {
            return false;
        }
        return lower.contains("stair")
                || lower.contains("lift")
                || lower.contains("elevator")
                || lower.contains("bridge")
                || lower.contains("tangga")
                || lower.contains("lif");
    }

    @NonNull
    private static GraphNode insertProjectedNode(
            @NonNull FloorGraph graph,
            @NonNull String virtualId,
            float px,
            float py) {

        Map<String, GraphNode> nodeMap = new HashMap<>();
        for (GraphNode n : graph.nodes) {
            nodeMap.put(n.id, n);
        }

        GraphEdge bestEdge = null;
        float bestProjX = px;
        float bestProjY = py;
        float minDist = Float.MAX_VALUE;

        for (GraphEdge edge : graph.edges) {
            GraphNode a = nodeMap.get(edge.from);
            GraphNode b = nodeMap.get(edge.to);
            if (a == null || b == null) continue;

            float dx = b.xPct - a.xPct;
            float dy = b.yPct - a.yPct;
            float lenSq = (dx * dx) + (dy * dy);
            float t = 0f;
            if (lenSq > 0.000001f) {
                t = (((px - a.xPct) * dx) + ((py - a.yPct) * dy)) / lenSq;
                t = Math.max(0f, Math.min(1f, t));
            }
            float projX = a.xPct + (t * dx);
            float projY = a.yPct + (t * dy);
            float d = euclideanDistance(px, py, projX, projY);

            if (d < minDist) {
                minDist = d;
                bestEdge = edge;
                bestProjX = projX;
                bestProjY = projY;
            }
        }

        if (bestEdge == null) {
            GraphNode fallback = findClosestNode(graph.nodes, px, py);
            return fallback != null ? fallback : new GraphNode(virtualId, px, py, virtualId);
        }

        GraphNode virtualNode = new GraphNode(virtualId, bestProjX, bestProjY, virtualId);
        graph.nodes.add(virtualNode);

        GraphNode u = nodeMap.get(bestEdge.from);
        GraphNode v = nodeMap.get(bestEdge.to);
        if (u != null && v != null) {
            float d1 = Math.max(0.0001f, euclideanDistance(u.xPct, u.yPct, bestProjX, bestProjY));
            float d2 = Math.max(0.0001f, euclideanDistance(bestProjX, bestProjY, v.xPct, v.yPct));
            graph.edges.add(new GraphEdge(u.id, virtualId, d1));
            graph.edges.add(new GraphEdge(virtualId, v.id, d2));
        }

        return virtualNode;
    }

    @NonNull
    public static List<GraphNode> computeShortestPath(
            @NonNull FloorGraph graph,
            @NonNull String startNodeId,
            @NonNull String targetNodeId) {

        Map<String, GraphNode> nodeMap = new HashMap<>();
        Map<String, List<GraphEdge>> adjacency = new HashMap<>();
        Map<String, Float> dist = new HashMap<>();
        Map<String, String> previous = new HashMap<>();

        for (GraphNode node : graph.nodes) {
            nodeMap.put(node.id, node);
            adjacency.put(node.id, new ArrayList<>());
            dist.put(node.id, Float.MAX_VALUE);
        }

        if (!nodeMap.containsKey(startNodeId) || !nodeMap.containsKey(targetNodeId)) {
            return Collections.emptyList();
        }

        for (GraphEdge edge : graph.edges) {
            GraphNode u = nodeMap.get(edge.from);
            GraphNode v = nodeMap.get(edge.to);
            if (u == null || v == null) continue;
            float w = edge.weight > 0f ? edge.weight : euclideanDistance(u.xPct, u.yPct, v.xPct, v.yPct);
            List<GraphEdge> listU = adjacency.get(u.id);
            List<GraphEdge> listV = adjacency.get(v.id);
            if (listU != null) listU.add(new GraphEdge(u.id, v.id, w));
            if (listV != null) listV.add(new GraphEdge(v.id, u.id, w));
        }

        dist.put(startNodeId, 0f);
        PriorityQueue<NodeRecord> pq = new PriorityQueue<>(Comparator.comparingDouble(r -> r.distance));
        pq.add(new NodeRecord(startNodeId, 0f));

        while (!pq.isEmpty()) {
            NodeRecord current = pq.poll();
            if (current == null) continue;
            if (current.nodeId.equals(targetNodeId)) break;

            float currentBest = getDist(dist, current.nodeId);
            if (current.distance > currentBest) continue;

            List<GraphEdge> neighbors = adjacency.get(current.nodeId);
            if (neighbors == null) continue;

            for (GraphEdge edge : neighbors) {
                float alt = currentBest + edge.weight;
                if (alt < getDist(dist, edge.to)) {
                    dist.put(edge.to, alt);
                    previous.put(edge.to, current.nodeId);
                    pq.add(new NodeRecord(edge.to, alt));
                }
            }
        }

        if (getDist(dist, targetNodeId) == Float.MAX_VALUE) {
            return Collections.emptyList();
        }

        List<GraphNode> result = new ArrayList<>();
        String step = targetNodeId;
        while (step != null) {
            GraphNode node = nodeMap.get(step);
            if (node != null) result.add(node);
            step = previous.get(step);
        }
        Collections.reverse(result);
        return result;
    }

    private static float getDist(@NonNull Map<String, Float> distMap, @NonNull String nodeId) {
        Float val = distMap.get(nodeId);
        return val != null ? val : Float.MAX_VALUE;
    }

    @NonNull
    private static FloorGraph cloneGraph(@NonNull FloorGraph src) {
        FloorGraph copy = new FloorGraph();
        for (GraphNode n : src.nodes) {
            copy.nodes.add(new GraphNode(n.id, n.xPct, n.yPct, n.label));
        }
        for (GraphEdge e : src.edges) {
            copy.edges.add(new GraphEdge(e.from, e.to, e.weight));
        }
        return copy;
    }

    /**
     * Simplifies grid micro-steps and collinear waypoints using Ramer-Douglas-Peucker
     * (epsilon = 0.004 normalized units) so auto-extracted bitmap paths render as straight lines.
     */
    @NonNull
    private static List<GraphNode> simplifyCollinearNodes(@NonNull List<GraphNode> raw) {
        if (raw.size() <= 2) return raw;
        List<GraphNode> filtered = new ArrayList<>();
        filtered.add(raw.get(0));
        for (int i = 1; i < raw.size() - 1; i++) {
            GraphNode prev = filtered.get(filtered.size() - 1);
            GraphNode curr = raw.get(i);
            if (euclideanDistance(prev.xPct, prev.yPct, curr.xPct, curr.yPct) >= 0.004f) {
                filtered.add(curr);
            }
        }
        filtered.add(raw.get(raw.size() - 1));
        return rdpSimplify(filtered, 0.004f);
    }

    @NonNull
    private static List<GraphNode> rdpSimplify(@NonNull List<GraphNode> pts, float epsilon) {
        if (pts.size() < 3) return pts;
        int index = -1;
        float maxDist = 0f;
        GraphNode first = pts.get(0);
        GraphNode last = pts.get(pts.size() - 1);

        for (int i = 1; i < pts.size() - 1; i++) {
            float d = perpendicularDistance(pts.get(i), first, last);
            if (d > maxDist) {
                index = i;
                maxDist = d;
            }
        }

        if (maxDist > epsilon && index != -1) {
            List<GraphNode> left = rdpSimplify(pts.subList(0, index + 1), epsilon);
            List<GraphNode> right = rdpSimplify(pts.subList(index, pts.size()), epsilon);
            List<GraphNode> result = new ArrayList<>(left.subList(0, left.size() - 1));
            result.addAll(right);
            return result;
        } else {
            List<GraphNode> result = new ArrayList<>();
            result.add(first);
            result.add(last);
            return result;
        }
    }

    private static float perpendicularDistance(
            @NonNull GraphNode p, @NonNull GraphNode a, @NonNull GraphNode b) {
        float dx = b.xPct - a.xPct;
        float dy = b.yPct - a.yPct;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.00001f) {
            return euclideanDistance(p.xPct, p.yPct, a.xPct, a.yPct);
        }
        return Math.abs((dy * p.xPct) - (dx * p.yPct) + (b.xPct * a.yPct) - (b.yPct * a.xPct)) / len;
    }

    private static int countSignificantTurns(@NonNull List<GraphNode> path) {
        if (path.size() < 3) return 0;
        int turns = 0;
        for (int i = 1; i < path.size() - 1; i++) {
            GraphNode a = path.get(i - 1);
            GraphNode b = path.get(i);
            GraphNode c = path.get(i + 1);
            double ang1 = Math.atan2(b.yPct - a.yPct, b.xPct - a.xPct);
            double ang2 = Math.atan2(c.yPct - b.yPct, c.xPct - b.xPct);
            double diff = Math.abs(Math.toDegrees(ang2 - ang1));
            if (diff > 180.0) {
                diff = 360.0 - diff;
            }
            if (diff > 35.0) turns++;
        }
        return turns;
    }

    @Nullable
    private static GraphNode findClosestNode(@NonNull List<GraphNode> nodes, float xPct, float yPct) {
        GraphNode closest = null;
        float minDistance = Float.MAX_VALUE;
        for (GraphNode node : nodes) {
            float d = euclideanDistance(xPct, yPct, node.xPct, node.yPct);
            if (d < minDistance) {
                minDistance = d;
                closest = node;
            }
        }
        return closest;
    }

    private static float calculatePathCost(@NonNull List<GraphNode> path) {
        float total = 0f;
        for (int i = 0; i < path.size() - 1; i++) {
            GraphNode a = path.get(i);
            GraphNode b = path.get(i + 1);
            total += euclideanDistance(a.xPct, a.yPct, b.xPct, b.yPct);
        }
        return total;
    }
}