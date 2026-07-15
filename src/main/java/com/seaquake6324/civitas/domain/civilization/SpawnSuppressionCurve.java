package com.seaquake6324.civitas.domain.civilization;

import java.util.List;

/** Piecewise-linear civility curve plus the independent activity effectiveness modifier. */
public final class SpawnSuppressionCurve {
    public record Point(double civility, double suppression) {}
    public record Breakdown(double baseSuppression, double activityModifier, double finalSuppression) {}

    private final List<Point> points;

    public SpawnSuppressionCurve(List<Point> points) {
        if (points == null || points.size() < 2) throw new IllegalArgumentException("At least two curve points are required");
        this.points = points.stream().sorted(java.util.Comparator.comparingDouble(Point::civility)).toList();
    }

    public double base(double civility) {
        if (civility <= points.getFirst().civility) return points.getFirst().suppression;
        for (int i = 1; i < points.size(); i++) {
            Point right = points.get(i);
            if (civility > right.civility) continue;
            Point left = points.get(i - 1);
            double t = (civility - left.civility) / (right.civility - left.civility);
            return left.suppression + (right.suppression - left.suppression) * t;
        }
        return points.getLast().suppression;
    }

    public double effective(double civility, double activity) {
        return breakdown(civility, activity).finalSuppression();
    }

    public Breakdown breakdown(double civility, double activity) {
        double baseSuppression = base(civility);
        double activityModifier = 0.4 + Math.max(0, Math.min(100, activity)) * 0.006;
        double finalSuppression = Math.max(0, Math.min(0.9, baseSuppression * activityModifier));
        return new Breakdown(baseSuppression, activityModifier, finalSuppression);
    }
}
