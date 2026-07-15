package com.seaquake6324.civitas.domain.region;

import java.util.Locale;

public record RegionDiagnostics(RegionType type, double undergroundConfidence, RegionEvidence evidence,
        RegionClassificationReason reason) {
    public String compactText() {
        RegionEvidence e = evidence;
        String path = e.outdoorReachable() ? Integer.toString(e.outdoorPathDistance()) : "unreached";
        return String.format(Locale.ROOT,
                "%s  underground=%.2f | surfaceY=%d depth=%d | sky=%d/9 | outdoorPath=%s | cover=%d (max %d) | enclosure=%.2f | BFS=%d%s | %s",
                type, undergroundConfidence, e.localSurfaceMedian(), e.burialDepth(), e.skyExposedSamples(), path,
                e.coverageMedian(), e.coverageMaximum(), e.enclosureRatio(), e.visitedNodes(),
                e.searchExhausted() ? " capped" : "", reason.compactText());
    }
}
