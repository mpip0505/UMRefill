package com.umrefill.app.util.pathfinding;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Automatically extracts a strictly-interior, centerline-weighted FloorGraph from a UM Refill
 * floor plan Bitmap. Uses adaptive building-footprint bounds detection (supporting FR5.3 uploads),
 * exterior-air flood-fill to strip outer building border halos, medial-axis wall penalties to keep
 * paths centered, and graph-level component bridging.
 */
public final class BitmapCorridorExtractor {

    private static final int GRID_SIZE = 96;
    private static final int MIN_ISLAND_CELLS = 5;
    private static final int MIN_ADJACENT_ROOM_CELLS = 3;
    private static final float MAX_GRAPH_BRIDGE_DIST_PCT = 0.28f;
    private static final float WALL_HUG_PENALTY = 1.45f;

    private BitmapCorridorExtractor() {}

    @NonNull
    public static FloorGraph extractFromBitmap(@NonNull Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        boolean[][] roomGrid = new boolean[GRID_SIZE][GRID_SIZE];
        boolean[][] whiteBgGrid = new boolean[GRID_SIZE][GRID_SIZE];
        boolean[][] greyCorridorGrid = new boolean[GRID_SIZE][GRID_SIZE];

        // 1. Sample 3x3 patches across the full 96x96 grid (no hardcoded faculty crop limits)
        for (int gy = 1; gy < GRID_SIZE - 1; gy++) {
            for (int gx = 1; gx < GRID_SIZE - 1; gx++) {
                int corridorHits = 0;
                int roomHits = 0;
                int whiteHits = 0;

                int centerPx = clamp((int) (((gx + 0.5f) / GRID_SIZE) * width), 0, width - 1);
                int centerPy = clamp((int) (((gy + 0.5f) / GRID_SIZE) * height), 0, height - 1);
                int centerColor = bitmap.getPixel(centerPx, centerPy);
                boolean centerIsRoom = isColoredRoomPixel(centerColor);

                for (int sy = -1; sy <= 1; sy++) {
                    float vPct = (gy + 0.5f + (sy * 0.25f)) / GRID_SIZE;
                    int py = clamp((int) (vPct * height), 0, height - 1);
                    for (int sx = -1; sx <= 1; sx++) {
                        float uPct = (gx + 0.5f + (sx * 0.25f)) / GRID_SIZE;
                        int px = clamp((int) (uPct * width), 0, width - 1);
                        int pixel = bitmap.getPixel(px, py);

                        if (isPureWhitePixel(pixel)) {
                            whiteHits++;
                        } else if (isColoredRoomPixel(pixel)) {
                            roomHits++;
                        } else if (isCorridorOrStairPixel(pixel)) {
                            corridorHits++;
                        }
                    }
                }

                roomGrid[gy][gx] = centerIsRoom || (roomHits >= 4);
                whiteBgGrid[gy][gx] = !roomGrid[gy][gx] && (whiteHits >= 6);
                greyCorridorGrid[gy][gx] = !roomGrid[gy][gx] && (corridorHits >= 3);
            }
        }

        // 2. Dynamically detect the active building bounds, excluding detached header/legend bands if present
        int[] bounds = detectActiveBuildingBounds(roomGrid, greyCorridorGrid);
        int minX = bounds[0];
        int maxX = bounds[1];
        int minY = bounds[2];
        int maxY = bounds[3];

        // 3. Flood-fill Exterior Air from the outer perimeter of the active bounds
        boolean[][] exteriorAir = markExteriorAir(whiteBgGrid, roomGrid, minX, maxX, minY, maxY);

        // 4. Build initial interior walkable grid, stripping anti-aliased outer building wall halos
        boolean[][] rawWalkable = new boolean[GRID_SIZE][GRID_SIZE];
        for (int y = minY + 1; y < maxY - 1; y++) {
            for (int x = minX + 1; x < maxX - 1; x++) {
                if (roomGrid[y][x] || exteriorAir[y][x]) continue;

                boolean sandwiched = isSandwichedBetweenRooms(roomGrid, x, y, minX, maxX, minY, maxY);
                boolean touchesOutsideAir = isAdjacentToExteriorAir(exteriorAir, x, y);

                // Strip outer wall border halos: a cell touching exterior air is only valid if tightly sandwiched
                if (touchesOutsideAir && !sandwiched) {
                    continue;
                }

                if (greyCorridorGrid[y][x] || sandwiched) {
                    rawWalkable[y][x] = true;
                }
            }
        }

        // 5. Morphological 3x3 closing strictly inside the building to heal text labels ("Laluan keluar", "Foyer")
        boolean[][] closed = new boolean[GRID_SIZE][GRID_SIZE];
        for (int y = minY + 1; y < maxY - 1; y++) {
            for (int x = minX + 1; x < maxX - 1; x++) {
                if (roomGrid[y][x] || exteriorAir[y][x]) continue;
                if (isAdjacentToExteriorAir(exteriorAir, x, y)
                        && !isSandwichedBetweenRooms(roomGrid, x, y, minX, maxX, minY, maxY)) {
                    continue;
                }
                int neighbors = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (rawWalkable[y + dy][x + dx]) neighbors++;
                    }
                }
                closed[y][x] = rawWalkable[y][x] || (neighbors >= 5);
            }
        }

        // 6. Prune tiny isolated noise specks and legend icons not bordering room blocks
        boolean[][] cleanWalkable = retainValidIslands(closed, roomGrid, minX, maxX, minY, maxY);

        // 7. Identify wall-adjacent cells so we can penalize wall-hugging ("fan meetups at borders")
        boolean[][] nearWall = new boolean[GRID_SIZE][GRID_SIZE];
        for (int y = minY + 1; y < maxY - 1; y++) {
            for (int x = minX + 1; x < maxX - 1; x++) {
                if (!cleanWalkable[y][x]) continue;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (!cleanWalkable[y + dy][x + dx]) {
                            nearWall[y][x] = true;
                            break;
                        }
                    }
                    if (nearWall[y][x]) break;
                }
            }
        }

        // 8. Build 8-directional FloorGraph with Medial-Axis Centerline weighting
        FloorGraph graph = new FloorGraph();
        float baseStep = 1.0f / GRID_SIZE;
        float baseDiag = 1.4142f / GRID_SIZE;

        for (int y = minY + 1; y < maxY - 1; y++) {
            for (int x = minX + 1; x < maxX - 1; x++) {
                if (!cleanWalkable[y][x]) continue;
                String id = nodeId(x, y);
                float xPct = (x + 0.5f) / GRID_SIZE;
                float yPct = (y + 0.5f) / GRID_SIZE;
                graph.nodes.add(new GraphNode(id, xPct, yPct, null));

                if (cleanWalkable[y][x + 1]) {
                    float mult = (nearWall[y][x] || nearWall[y][x + 1]) ? WALL_HUG_PENALTY : 1.0f;
                    graph.edges.add(new GraphEdge(id, nodeId(x + 1, y), baseStep * mult));
                }
                if (cleanWalkable[y + 1][x]) {
                    float mult = (nearWall[y][x] || nearWall[y + 1][x]) ? WALL_HUG_PENALTY : 1.0f;
                    graph.edges.add(new GraphEdge(id, nodeId(x, y + 1), baseStep * mult));
                }
                if (cleanWalkable[y + 1][x + 1] && cleanWalkable[y][x + 1] && cleanWalkable[y + 1][x]) {
                    float mult = (nearWall[y][x] || nearWall[y + 1][x + 1]) ? WALL_HUG_PENALTY : 1.0f;
                    graph.edges.add(new GraphEdge(id, nodeId(x + 1, y + 1), baseDiag * mult));
                }
                if (cleanWalkable[y + 1][x - 1] && cleanWalkable[y][x - 1] && cleanWalkable[y + 1][x]) {
                    float mult = (nearWall[y][x] || nearWall[y + 1][x - 1]) ? WALL_HUG_PENALTY : 1.0f;
                    graph.edges.add(new GraphEdge(id, nodeId(x - 1, y + 1), baseDiag * mult));
                }
            }
        }

        // 9. Graph-Level Connected Component Bridging: guarantees staircase choke-points
        //    (e.g. Laluan keluar / To Bakawali -> Hunian Cipta -> Hunian Tamah) have valid GraphEdges
        bridgeDisconnectedGraphComponents(graph);

        return graph;
    }

    /**
     * Dynamically detects the vertical and horizontal bounding box of the main building footprint
     * by locating the largest contiguous vertical band of rows containing structural room blocks.
     * Supports both template maps (with headers/legends) and raw full-bleed floor plans (FR5.3).
     */
    @NonNull
    private static int[] detectActiveBuildingBounds(
            @NonNull boolean[][] roomGrid,
            @NonNull boolean[][] greyCorridorGrid) {
        int[] rowRoomCounts = new int[GRID_SIZE];
        for (int y = 1; y < GRID_SIZE - 1; y++) {
            int count = 0;
            for (int x = 1; x < GRID_SIZE - 1; x++) {
                if (roomGrid[y][x] || greyCorridorGrid[y][x]) {
                    count++;
                }
            }
            rowRoomCounts[y] = count;
        }

        // Find the longest vertical run of rows with active building structure (bridging 1-row gaps)
        int bestStartY = 1;
        int bestEndY = GRID_SIZE - 1;
        int bestMass = 0;

        int curStart = -1;
        int curMass = 0;
        int emptyGap = 0;
        for (int y = 1; y < GRID_SIZE - 1; y++) {
            if (rowRoomCounts[y] >= 4) {
                if (curStart == -1) curStart = y;
                curMass += rowRoomCounts[y];
                emptyGap = 0;
                if (curMass > bestMass) {
                    bestMass = curMass;
                    bestStartY = curStart;
                    bestEndY = y + 1;
                }
            } else if (curStart != -1) {
                emptyGap++;
                if (emptyGap >= 3) {
                    curStart = -1;
                    curMass = 0;
                    emptyGap = 0;
                }
            }
        }

        int minY = Math.max(1, bestStartY - 2);
        int maxY = Math.min(GRID_SIZE - 1, bestEndY + 2);
        return new int[]{1, GRID_SIZE - 1, minY, maxY};
    }

    /**
     * Flood-fills exterior white background starting from the top/bottom/left/right frame borders.
     * Stops at room walls and narrow sandwiched corridors so interior white corridors are preserved.
     */
    @NonNull
    private static boolean[][] markExteriorAir(
            @NonNull boolean[][] whiteBgGrid,
            @NonNull boolean[][] roomGrid,
            int minX, int maxX, int minY, int maxY) {
        boolean[][] exterior = new boolean[GRID_SIZE][GRID_SIZE];
        Queue<int[]> queue = new ArrayDeque<>();

        for (int x = minX; x < maxX; x++) {
            if (!roomGrid[minY][x]) {
                exterior[minY][x] = true;
                queue.add(new int[]{x, minY});
            }
            if (!roomGrid[maxY - 1][x]) {
                exterior[maxY - 1][x] = true;
                queue.add(new int[]{x, maxY - 1});
            }
        }
        for (int y = minY; y < maxY; y++) {
            if (!roomGrid[y][minX]) {
                exterior[y][minX] = true;
                queue.add(new int[]{minX, y});
            }
            if (!roomGrid[y][maxX - 1]) {
                exterior[y][maxX - 1] = true;
                queue.add(new int[]{maxX - 1, y});
            }
        }

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int cx = curr[0];
            int cy = curr[1];
            for (int d = 0; d < 4; d++) {
                int nx = cx + dx[d];
                int ny = cy + dy[d];
                if (nx < minX || nx >= maxX || ny < minY || ny >= maxY) continue;
                if (exterior[ny][nx] || roomGrid[ny][nx]) continue;
                if (whiteBgGrid[ny][nx] && !isSandwichedBetweenRooms(roomGrid, nx, ny, minX, maxX, minY, maxY)) {
                    exterior[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return exterior;
    }

    private static boolean isAdjacentToExteriorAir(@NonNull boolean[][] exteriorAir, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (exteriorAir[y + dy][x + dx]) return true;
            }
        }
        return false;
    }

    /**
     * Detects interior hallways flanked on opposite sides (Left+Right or Top+Bottom) by colored rooms.
     */
    private static boolean isSandwichedBetweenRooms(
            @NonNull boolean[][] roomGrid, int x, int y, int minX, int maxX, int minY, int maxY) {
        boolean roomLeft = false;
        boolean roomRight = false;
        for (int d = 1; d <= 3; d++) {
            if (x - d >= minX && roomGrid[y][x - d]) roomLeft = true;
            if (x + d < maxX && roomGrid[y][x + d]) roomRight = true;
        }
        if (roomLeft && roomRight) return true;

        boolean roomUp = false;
        boolean roomDown = false;
        for (int d = 1; d <= 3; d++) {
            if (y - d >= minY && roomGrid[y - d][x]) roomUp = true;
            if (y + d < maxY && roomGrid[y + d][x]) roomDown = true;
        }
        return roomUp && roomDown;
    }

    /**
     * Ensures all valid corridor islands in the FloorGraph are connected into a single graph
     * by inserting direct corridor-bridge GraphEdges across narrow staircase choke-points.
     */
    private static void bridgeDisconnectedGraphComponents(@NonNull FloorGraph graph) {
        if (graph.nodes.size() < 2) return;

        while (true) {
            List<List<GraphNode>> components = findGraphComponents(graph);
            if (components.size() <= 1) break;

            components.sort((a, b) -> Integer.compare(b.size(), a.size()));
            List<GraphNode> mainComp = components.get(0);

            GraphNode bestMainNode = null;
            GraphNode bestOtherNode = null;
            float bestDist = Float.MAX_VALUE;

            for (int i = 1; i < components.size(); i++) {
                for (GraphNode other : components.get(i)) {
                    for (GraphNode main : mainComp) {
                        float d = Pathfinder.euclideanDistance(other.xPct, other.yPct, main.xPct, main.yPct);
                        if (d < bestDist) {
                            bestDist = d;
                            bestMainNode = main;
                            bestOtherNode = other;
                        }
                    }
                }
            }

            if (bestMainNode == null || bestOtherNode == null || bestDist > MAX_GRAPH_BRIDGE_DIST_PCT) {
                break;
            }

            // Connect via an orthogonal L-bend (or straight segment if aligned) so it never cuts diagonally
            if (Math.abs(bestMainNode.xPct - bestOtherNode.xPct) < 0.015f
                    || Math.abs(bestMainNode.yPct - bestOtherNode.yPct) < 0.015f) {
                graph.edges.add(new GraphEdge(bestMainNode.id, bestOtherNode.id, bestDist));
            } else {
                String cornerId = "bridge_" + graph.nodes.size();
                GraphNode corner = new GraphNode(cornerId, bestOtherNode.xPct, bestMainNode.yPct, null);
                graph.nodes.add(corner);
                float d1 = Math.max(0.001f, Math.abs(bestOtherNode.yPct - corner.yPct));
                float d2 = Math.max(0.001f, Math.abs(bestMainNode.xPct - corner.xPct));
                graph.edges.add(new GraphEdge(bestOtherNode.id, cornerId, d1));
                graph.edges.add(new GraphEdge(cornerId, bestMainNode.id, d2));
            }
        }
    }

    @NonNull
    private static List<List<GraphNode>> findGraphComponents(@NonNull FloorGraph graph) {
        Map<String, GraphNode> nodeMap = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (GraphNode n : graph.nodes) {
            nodeMap.put(n.id, n);
            adj.put(n.id, new ArrayList<>());
        }
        for (GraphEdge e : graph.edges) {
            List<String> listU = adj.get(e.from);
            List<String> listV = adj.get(e.to);
            if (listU != null) listU.add(e.to);
            if (listV != null) listV.add(e.from);
        }

        Map<String, Boolean> visited = new HashMap<>();
        List<List<GraphNode>> comps = new ArrayList<>();

        for (GraphNode start : graph.nodes) {
            if (Boolean.TRUE.equals(visited.get(start.id))) continue;
            List<GraphNode> comp = new ArrayList<>();
            Queue<String> q = new ArrayDeque<>();
            q.add(start.id);
            visited.put(start.id, true);

            while (!q.isEmpty()) {
                String currId = q.poll();
                GraphNode currNode = nodeMap.get(currId);
                if (currNode != null) comp.add(currNode);
                List<String> neighbors = adj.get(currId);
                if (neighbors == null) continue;
                for (String nxt : neighbors) {
                    if (!Boolean.TRUE.equals(visited.get(nxt))) {
                        visited.put(nxt, true);
                        q.add(nxt);
                    }
                }
            }
            comps.add(comp);
        }
        return comps;
    }

    @NonNull
    private static boolean[][] retainValidIslands(
            @NonNull boolean[][] grid,
            @NonNull boolean[][] roomGrid,
            int minX, int maxX, int minY, int maxY) {
        boolean[][] visited = new boolean[GRID_SIZE][GRID_SIZE];
        boolean[][] out = new boolean[GRID_SIZE][GRID_SIZE];
        int[] dx = {-1, 0, 1, -1, 1, -1, 0, 1};
        int[] dy = {-1, -1, -1, 0, 0, 1, 1, 1};

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if (!grid[y][x] || visited[y][x]) continue;
                List<int[]> comp = new ArrayList<>();
                Queue<int[]> q = new ArrayDeque<>();
                q.add(new int[]{x, y});
                visited[y][x] = true;
                int adjacentRooms = 0;

                while (!q.isEmpty()) {
                    int[] curr = q.poll();
                    comp.add(curr);
                    for (int d = 0; d < 8; d++) {
                        int nx = curr[0] + dx[d];
                        int ny = curr[1] + dy[d];
                        if (nx >= minX && nx < maxX && ny >= minY && ny < maxY) {
                            if (roomGrid[ny][nx]) {
                                adjacentRooms++;
                            }
                            if (grid[ny][nx] && !visited[ny][nx]) {
                                visited[ny][nx] = true;
                                q.add(new int[]{nx, ny});
                            }
                        }
                    }
                }
                if (comp.size() >= MIN_ISLAND_CELLS && adjacentRooms >= MIN_ADJACENT_ROOM_CELLS) {
                    for (int[] c : comp) {
                        out[c[1]][c[0]] = true;
                    }
                }
            }
        }
        return out;
    }

    private static boolean isPureWhitePixel(int color) {
        return Color.red(color) >= 251 && Color.green(color) >= 251 && Color.blue(color) >= 251;
    }

    private static boolean isCorridorOrStairPixel(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        if (r >= 252 && g >= 252 && b >= 252) return false;

        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        int chroma = max - min;
        return max >= 195 && max <= 251 && chroma <= 20 && (b >= r - 2);
    }

    private static boolean isColoredRoomPixel(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        int chroma = max - min;

        if (chroma > 20) return true;
        if ((g - r) >= 6 && (g - b) >= 5) return true; // Sage green offices (#D4E6D5)
        if ((r - b) >= 10) return true;                // Warm yellow halls / orange toilets
        return false;
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    @NonNull
    private static String nodeId(int gx, int gy) {
        return "g_" + gx + "_" + gy;
    }
}