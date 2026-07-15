package com.seaquake6324.civitas.domain.region;

public final class RegionClassificationRules {
    public record Settings(int surfacePath, int interiorPath, int shallowDepth, int undergroundDepth,
                           int thinCoverage, int undergroundCoverage, double enclosureThreshold,
                           double undergroundScoreThreshold) {}

    public static RegionDiagnostics classify(RegionEvidence evidence, Settings settings) {
        boolean shortOutdoorPath = evidence.outdoorReachable() && evidence.outdoorPathDistance() <= settings.surfacePath();
        boolean visiblyOpen = evidence.skyExposedSamples() >= 2;
        boolean shallow = evidence.burialDepth() <= settings.shallowDepth();
        boolean thinRoof = evidence.coverageMedian() <= settings.thinCoverage();

        // Ravines, open shafts, cave mouths, porches and eaves remain surface while they retain a short open route.
        if (shortOutdoorPath || visiblyOpen) {
            return result(RegionType.SURFACE, score(evidence, settings), evidence,
                    shortOutdoorPath ? RegionClassificationReason.SHORT_OUTDOOR_ROUTE
                            : RegionClassificationReason.OPEN_SKY_SAMPLING);
        }

        boolean interiorRoute = evidence.outdoorReachable() && evidence.outdoorPathDistance() <= settings.interiorPath();
        if (shallow && thinRoof && interiorRoute) {
            return result(RegionType.INTERIOR, score(evidence, settings), evidence,
                    RegionClassificationReason.SHALLOW_THIN_ROOF_OUTDOOR_ROUTE);
        }

        double confidence = score(evidence, settings);
        boolean buried = evidence.burialDepth() >= settings.undergroundDepth();
        boolean thickCover = evidence.coverageMedian() >= settings.undergroundCoverage();
        boolean disconnected = !evidence.outdoorReachable() || evidence.outdoorPathDistance() > settings.interiorPath();
        boolean enclosed = evidence.enclosureRatio() >= settings.enclosureThreshold();
        if (buried && thickCover && disconnected && enclosed && confidence >= settings.undergroundScoreThreshold()) {
            return result(RegionType.UNDERGROUND, confidence, evidence,
                    RegionClassificationReason.BURIED_THICK_COVERED_ENCLOSED_DISCONNECTED);
        }

        // A thin constructed roof is an explicit anti-underground guard, including very large roofs where BFS hits its bound.
        RegionClassificationReason reason = thinRoof ? RegionClassificationReason.THIN_OR_MAN_MADE_ROOF_GUARD
                : RegionClassificationReason.INSUFFICIENT_INDEPENDENT_UNDERGROUND_EVIDENCE;
        return result(RegionType.INTERIOR, confidence, evidence, reason);
    }

    private static double score(RegionEvidence e, Settings s) {
        double burial = clamp((double)e.burialDepth() / Math.max(1, s.undergroundDepth() * 2));
        double cover = clamp((double)e.coverageMedian() / Math.max(1, s.undergroundCoverage() * 2));
        double disconnected = !e.outdoorReachable() ? 1.0 : clamp((double)(e.outdoorPathDistance() - s.surfacePath()) / Math.max(1, s.interiorPath()));
        double noSky = 1.0 - e.skyExposedSamples() / 9.0;
        return clamp(burial * 0.28 + cover * 0.27 + disconnected * 0.25 + e.enclosureRatio() * 0.12 + noSky * 0.08);
    }

    private static RegionDiagnostics result(RegionType type, double score, RegionEvidence evidence,
            RegionClassificationReason reason) {
        return new RegionDiagnostics(type, score, evidence, reason);
    }

    private static double clamp(double value) { return Math.max(0.0, Math.min(1.0, value)); }
    private RegionClassificationRules() {}
}
