package com.livingvillages.core.graph;

import com.livingvillages.core.data.Vec3i;

import java.util.*;

/**
 * 3D Constrained A* pathfinder for inter-cluster rail routing.
 *
 * <p>Searches in a 26-neighbor 3D grid with slope penalties.
 * Paths avoid sharp turns (>45° direction change) and penalize
 * steep terrain (|Δy|/horizDist > maxSlope).</p>
 *
 * <p>In the Core layer, terrain height at any (x,z) is approximated
 * from the start/end Y values since actual MC terrain data lives in
 * the Adapter layer. The Adapter can replace this with a terrain-aware
 * implementation.</p>
 */
final class ConstrainedAStar {

    private static final int HORIZONTAL_STEP = 16;  // chunk-aligned horizontal step
    private static final int VERTICAL_STEP = 1;

    /** Directions: 26 neighbors in 3D (dx, dy, dz), with horizontal step. */
    private static final int[][] DIRECTIONS = computeDirections();

    private ConstrainedAStar() {}

    /**
     * Result of an A* search.
     */
    record PathResult(List<Vec3i> path, boolean found) {
        static PathResult notFound() { return new PathResult(List.of(), false); }
    }

    /**
     * Find a path between two points with slope constraints.
     *
     * @param from     start position
     * @param to       target position
     * @param maxSlope maximum allowed Δy/horizontalDistance (from config)
     * @return path result with waypoints or empty if no path found
     */
    static PathResult findPath(Vec3i from, Vec3i to, double maxSlope) {
        return findPathInternal(from, to, maxSlope, 1000);
    }

    /**
     * Find with configurable node limit (for time-budgeted/framed execution).
     */
    static PathResult findPathWithLimit(Vec3i from, Vec3i to, double maxSlope, int nodeLimit) {
        return findPathInternal(from, to, maxSlope, nodeLimit);
    }

    private static PathResult findPathInternal(Vec3i from, Vec3i to, double maxSlope, int nodeLimit) {
        // A* open set: priority queue by f = g + h
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<Vec3i, Node> allNodes = new HashMap<>();

        Node start = new Node(from, 0, heuristic(from, to), null, null);
        openSet.add(start);
        allNodes.put(from, start);

        int explored = 0;

        while (!openSet.isEmpty() && explored < nodeLimit) {
            Node current = openSet.poll();
            explored++;

            if (current.pos.equals(to)) {
                return new PathResult(reconstructPath(current), true);
            }

            if (current.closed) continue;
            current.closed = true;

            for (int[] dir : DIRECTIONS) {
                Vec3i neighborPos = new Vec3i(
                        current.pos.x() + dir[0],
                        current.pos.y() + dir[1],
                        current.pos.z() + dir[2]);

                // Skip if already closed
                Node existing = allNodes.get(neighborPos);
                if (existing != null && existing.closed) continue;

                // Direction constraint: no sharp turns (>45°)
                if (current.prevDir != null) {
                    double angle = angleBetween(current.prevDir, dir);
                    if (angle > Math.PI / 4) { // > 45°
                        continue;
                    }
                }

                // Slope penalty
                double horizDist = Math.sqrt(dir[0] * dir[0] + dir[2] * dir[2]);
                double slope = horizDist > 0 ? Math.abs(dir[1]) / horizDist : 0;
                double stepCost = horizDist;
                if (slope > maxSlope) {
                    stepCost *= 10; // slope penalty
                }

                double tentativeG = current.g + stepCost;

                if (existing == null || tentativeG < existing.g) {
                    Node neighbor = new Node(neighborPos, tentativeG,
                            heuristic(neighborPos, to), current, dir);
                    allNodes.put(neighborPos, neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        // No path found within node limit
        if (explored >= nodeLimit) {
            // Return best-effort path to closest node to target
            Node best = null;
            double bestH = Double.MAX_VALUE;
            for (Node n : allNodes.values()) {
                double h = heuristic(n.pos, to);
                if (h < bestH) {
                    bestH = h;
                    best = n;
                }
            }
            if (best != null) {
                return new PathResult(reconstructPath(best), false);
            }
        }

        return PathResult.notFound();
    }

    /** Heuristic: 3D Euclidean distance. */
    private static double heuristic(Vec3i a, Vec3i b) {
        return Math.sqrt(a.distanceSq(b));
    }

    /** Reconstruct path from target node back to start. */
    private static List<Vec3i> reconstructPath(Node target) {
        List<Vec3i> path = new ArrayList<>();
        Node cur = target;
        while (cur != null) {
            path.add(cur.pos);
            cur = cur.parent;
        }
        Collections.reverse(path);
        return path;
    }

    /** Compute angle between two direction vectors in radians. */
    private static double angleBetween(int[] a, int[] b) {
        double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        double magA = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        double magB = Math.sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2]);
        if (magA == 0 || magB == 0) return 0;
        double cos = dot / (magA * magB);
        return Math.acos(Math.max(-1, Math.min(1, cos)));
    }

    /** Generate 26-neighbor directions with horizontal step size. */
    private static int[][] computeDirections() {
        int[] steps = {-HORIZONTAL_STEP, 0, HORIZONTAL_STEP};
        List<int[]> dirs = new ArrayList<>();
        for (int dx : steps) {
            for (int dy : new int[]{-VERTICAL_STEP, 0, VERTICAL_STEP}) {
                for (int dz : steps) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    dirs.add(new int[]{dx, dy, dz});
                }
            }
        }
        return dirs.toArray(new int[0][]);
    }

    /** A* search node. */
    private static class Node implements Comparable<Node> {
        final Vec3i pos;
        double g, f;
        boolean closed;
        final Node parent;
        final int[] prevDir;

        Node(Vec3i pos, double g, double h, Node parent, int[] prevDir) {
            this.pos = pos;
            this.g = g;
            this.f = g + h;
            this.parent = parent;
            this.prevDir = prevDir;
        }

        @Override
        public int compareTo(Node o) {
            return Double.compare(this.f, o.f);
        }
    }
}
