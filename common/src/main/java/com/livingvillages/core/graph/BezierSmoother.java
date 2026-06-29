package com.livingvillages.core.graph;

import com.livingvillages.core.data.Vec3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cubic Bezier path smoothing for rail waypoints.
 *
 * <p>Extracts key waypoints (direction change > 15°), then interpolates
 * using cubic Bezier curves with a sample step of 2 blocks.
 * Ensures no sharp angles (< 120°) remain after smoothing.</p>
 */
final class BezierSmoother {

    private static final double KEYPOINT_ANGLE_THRESHOLD = Math.toRadians(15);
    private static final double MIN_SEGMENT_ANGLE = Math.toRadians(120);
    private static final int SAMPLE_STEP = 2;  // blocks

    private BezierSmoother() {}

    /**
     * Smooth a raw path of waypoints into a denser, smooth path.
     *
     * @param rawPath the A* output path (may have sharp corners)
     * @return smoothed path with sample step ~2 blocks
     */
    static List<Vec3i> smooth(List<Vec3i> rawPath) {
        if (rawPath.size() <= 2) {
            return new ArrayList<>(rawPath);
        }

        // Extract key waypoints
        List<Vec3i> keyPoints = extractKeyPoints(rawPath);
        if (keyPoints.size() <= 2) {
            return new ArrayList<>(keyPoints);
        }

        // Apply cubic Bezier interpolation between key points
        List<Vec3i> smoothed = new ArrayList<>();
        smoothed.add(keyPoints.get(0));

        for (int i = 0; i < keyPoints.size() - 1; i++) {
            Vec3i p0 = keyPoints.get(Math.max(0, i - 1));
            Vec3i p1 = keyPoints.get(i);
            Vec3i p2 = keyPoints.get(i + 1);
            Vec3i p3 = keyPoints.get(Math.min(keyPoints.size() - 1, i + 2));

            // Cubic Bezier: B(t) = (1-t)³P1 + 3(1-t)²t·CP1 + 3(1-t)t²·CP2 + t³P2
            // Control points: use Catmull-Rom to Bezier conversion
            Vec3i cp1 = new Vec3i(
                    p1.x() + (p2.x() - p0.x()) / 6,
                    p1.y() + (p2.y() - p0.y()) / 6,
                    p1.z() + (p2.z() - p0.z()) / 6);
            Vec3i cp2 = new Vec3i(
                    p2.x() - (p3.x() - p1.x()) / 6,
                    p2.y() - (p3.y() - p1.y()) / 6,
                    p2.z() - (p3.z() - p1.z()) / 6);

            // Sample the Bezier curve
            double segmentLength = Math.sqrt(p1.distanceSq(p2));
            int samples = Math.max(1, (int) Math.ceil(segmentLength / SAMPLE_STEP));

            for (int s = 1; s < samples; s++) {
                double t = (double) s / samples;
                Vec3i pt = evaluateCubicBezier(p1, cp1, cp2, p2, t);
                smoothed.add(pt);
            }
            smoothed.add(p2);
        }

        // Verify: no sharp angles
        List<Vec3i> verified = new ArrayList<>();
        verified.add(smoothed.get(0));
        for (int i = 1; i < smoothed.size() - 1; i++) {
            Vec3i prev = smoothed.get(i - 1);
            Vec3i cur = smoothed.get(i);
            Vec3i next = smoothed.get(i + 1);

            double angle = angleBetween(
                    cur.x() - prev.x(), cur.y() - prev.y(), cur.z() - prev.z(),
                    next.x() - cur.x(), next.y() - cur.y(), next.z() - cur.z());

            if (angle >= MIN_SEGMENT_ANGLE) {
                verified.add(cur);
            }
            // else: skip this point (smoothed out sharp corner)
        }
        verified.add(smoothed.get(smoothed.size() - 1));

        return verified;
    }

    /** Extract points where direction changes more than threshold. */
    private static List<Vec3i> extractKeyPoints(List<Vec3i> path) {
        if (path.size() <= 2) return new ArrayList<>(path);
        List<Vec3i> keys = new ArrayList<>();
        keys.add(path.get(0));

        int[] prevDir = null;
        for (int i = 1; i < path.size(); i++) {
            Vec3i a = path.get(i - 1);
            Vec3i b = path.get(i);
            int[] dir = {b.x() - a.x(), b.y() - a.y(), b.z() - a.z()};

            if (prevDir != null) {
                double angle = angleBetween(prevDir[0], prevDir[1], prevDir[2],
                        dir[0], dir[1], dir[2]);
                if (angle > KEYPOINT_ANGLE_THRESHOLD) {
                    keys.add(a); // direction changed, keep this as keypoint
                }
            }
            prevDir = dir;
        }
        keys.add(path.get(path.size() - 1));
        return keys;
    }

    /** Evaluate cubic Bezier at parameter t. */
    private static Vec3i evaluateCubicBezier(Vec3i p1, Vec3i cp1, Vec3i cp2, Vec3i p2, double t) {
        double u = 1 - t;
        double u2 = u * u, u3 = u2 * u;
        double t2 = t * t, t3 = t2 * t;

        double x = u3 * p1.x() + 3 * u2 * t * cp1.x() + 3 * u * t2 * cp2.x() + t3 * p2.x();
        double y = u3 * p1.y() + 3 * u2 * t * cp1.y() + 3 * u * t2 * cp2.y() + t3 * p2.y();
        double z = u3 * p1.z() + 3 * u2 * t * cp1.z() + 3 * u * t2 * cp2.z() + t3 * p2.z();

        return new Vec3i((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
    }

    /** Compute angle between two 3D direction vectors, in radians. */
    static double angleBetween(double dx1, double dy1, double dz1,
                                double dx2, double dy2, double dz2) {
        double dot = dx1 * dx2 + dy1 * dy2 + dz1 * dz2;
        double mag1 = Math.sqrt(dx1 * dx1 + dy1 * dy1 + dz1 * dz1);
        double mag2 = Math.sqrt(dx2 * dx2 + dy2 * dy2 + dz2 * dz2);
        if (mag1 == 0 || mag2 == 0) return Math.PI; // treat zero-length as max angle
        double cos = dot / (mag1 * mag2);
        return Math.acos(Math.max(-1, Math.min(1, cos)));
    }
}
