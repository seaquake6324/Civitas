package com.seaquake6324.civitas.domain.region;

public record RegionEvidence(
        int localSurfaceMedian,
        int burialDepth,
        int skyExposedSamples,
        int outdoorPathDistance,
        boolean outdoorReachable,
        int coverageMedian,
        int coverageMaximum,
        double enclosureRatio,
        int visitedNodes,
        boolean searchExhausted
) {}
