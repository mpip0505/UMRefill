package com.umrefill.app.util.pathfinding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.umrefill.app.data.entity.EntryPoint;
import com.umrefill.app.data.entity.PointStatus;
import com.umrefill.app.data.entity.PointType;
import com.umrefill.app.data.entity.RefillPoint;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for Dijkstra corridor pathfinding, orthogonal corridor-edge snapping,
 * working-point selection, and vertical exit-stair filtering (FR3.2).
 */
public class PathFinderTest {

    private static FloorGraph buildSampleLShapedCorridorGraph() {
        FloorGraph graph = new FloorGraph();
        graph.nodes.add(new GraphNode("n1", 0.10f, 0.80f, "West Entrance"));
        graph.nodes.add(new GraphNode("n2", 0.50f, 0.80f, "Corner Junction"));
        graph.nodes.add(new GraphNode("n3", 0.50f, 0.20f, "North Corridor End"));

        graph.edges.add(new GraphEdge("n1", "n2", 0.40f));
        graph.edges.add(new GraphEdge("n2", "n3", 0.60f));
        return graph;
    }

    @Test
    public void computeShortestPath_returnsOrderedCorridorNodes() {
        FloorGraph graph = buildSampleLShapedCorridorGraph();
        List<GraphNode> path = Pathfinder.computeShortestPath(graph, "n1", "n3");

        assertEquals(3, path.size());
        assertEquals("n1", path.get(0).id);
        assertEquals("n2", path.get(1).id);
        assertEquals("n3", path.get(2).id);
    }

    @Test
    public void snapEntryToCorridor_projectsOffCorridorTapOntoNearestEdge() {
        FloorGraph graph = buildSampleLShapedCorridorGraph();
        EntryPoint rawTap = new EntryPoint(-999L, 6L, "Tapped Position", 0.30f, 0.92f);

        EntryPoint snapped = Pathfinder.snapEntryToCorridor(rawTap, graph);
        assertEquals(0.30f, snapped.xPct, 0.001f);
        assertEquals(0.80f, snapped.yPct, 0.001f);
    }

    @Test
    public void findRouteToNearestWorking_skipsBrokenPointAndRoutesToWorkingPoint() {
        FloorGraph graph = buildSampleLShapedCorridorGraph();
        EntryPoint start = new EntryPoint(1L, 6L, "West Entrance", 0.10f, 0.80f);

        RefillPoint brokenCloser = new RefillPoint(
                10L, 6L, "Closer Broken Dispenser", PointType.FILTERED, PointStatus.BROKEN,
                0.25f, 0.80f, "Near West Entrance", 1000L, "UMR-TEST-01", true);
        RefillPoint workingFarther = new RefillPoint(
                11L, 6L, "Farther Working Cooler", PointType.COLD, PointStatus.WORKING,
                0.50f, 0.20f, "North End", 1000L, "UMR-TEST-02", true);

        Pathfinder.RouteResult route = Pathfinder.findRouteToNearestWorking(
                start, Arrays.asList(brokenCloser, workingFarther), graph);

        assertNotNull(route.targetPoint);
        assertEquals(11L, route.targetPoint.pointId);
        assertFalse(route.path.isEmpty());
        assertFalse(route.hasUnreachableWorkingPoints);
    }

    @Test
    public void findRouteToNearestWorking_flagsUnreachableWorkingPointOnDisconnectedGraph() {
        FloorGraph disconnectedGraph = new FloorGraph();
        disconnectedGraph.nodes.add(new GraphNode("a1", 0.10f, 0.80f, "Island A1"));
        disconnectedGraph.nodes.add(new GraphNode("a2", 0.20f, 0.80f, "Island A2"));
        disconnectedGraph.nodes.add(new GraphNode("b1", 0.80f, 0.20f, "Island B1"));
        disconnectedGraph.nodes.add(new GraphNode("b2", 0.90f, 0.20f, "Island B2"));
        disconnectedGraph.edges.add(new GraphEdge("a1", "a2", 0.10f));
        disconnectedGraph.edges.add(new GraphEdge("b1", "b2", 0.10f));

        EntryPoint start = new EntryPoint(1L, 6L, "West Stairs", 0.10f, 0.80f);
        RefillPoint unreachableWorking = new RefillPoint(
                12L, 6L, "Isolated Cooler", PointType.COLD, PointStatus.WORKING,
                0.85f, 0.20f, "Isolated Wing", 1000L, "UMR-TEST-03", true);

        Pathfinder.RouteResult route = Pathfinder.findRouteToNearestWorking(
                start, Collections.singletonList(unreachableWorking), disconnectedGraph);

        assertNull(route.targetPoint);
        assertTrue(route.hasUnreachableWorkingPoints);
    }

    @Test
    public void findRouteToNearestExitStairs_filtersOutCorridorsAndMainEntrance() {
        FloorGraph graph = buildSampleLShapedCorridorGraph();
        EntryPoint currentPos = new EntryPoint(-999L, 8L, "Custom Position", 0.15f, 0.80f);

        EntryPoint mainEntrance = new EntryPoint(81L, 8L, "Main Entrance (Foyer)", 0.18f, 0.80f);
        EntryPoint flatCorridor = new EntryPoint(85L, 8L, "Perpustakaan Corridor", 0.20f, 0.80f);
        EntryPoint actualStairs = new EntryPoint(82L, 8L, "North Stairs", 0.50f, 0.20f);

        Pathfinder.RouteResult exitRoute = Pathfinder.findRouteToNearestExitStairs(
                currentPos, Arrays.asList(mainEntrance, flatCorridor, actualStairs), graph);

        assertNotNull(exitRoute.targetExitStairs);
        assertEquals(82L, exitRoute.targetExitStairs.entryId);
    }
}