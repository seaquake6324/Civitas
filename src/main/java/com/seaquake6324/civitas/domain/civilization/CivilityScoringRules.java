package com.seaquake6324.civitas.domain.civilization;

import java.util.ArrayList;
import java.util.List;

/** Centralized first-pass 0.1d scoring curves. */
public final class CivilityScoringRules {
    public static CivilityScore score(CivilityEvidence e) {
        return score(e,new Weights(.35,.25,.25,.15));
    }
    public static CivilityScore score(CivilityEvidence e,Weights weights) {
        List<CivilityLimitReason> limits = new ArrayList<>();
        double building;
        if (e.standableCells() == 0 || e.passableCells() == 0) {
            building = 0;
            limits.add(CivilityLimitReason.NO_STANDABLE_SPACE);
        } else {
            double capacity = saturating(e.standableCells(), 64);
            double continuity = ratio(e.largestConnectedCells(), e.passableCells());
            building = 100 * (capacity * .40 + e.enclosureRatio() * .35 + continuity * .25);
        }

        int categories = 0;
        double facilities = 0;
        for (int count : e.facilityDistributionPoints()) {
            if (count > 0) { categories++; facilities += 10 + Math.max(0, count - 1) * 3; }
        }
        if (categories >= 4) facilities += 4;

        double safeRatio = ratio(e.safePassableCells(), e.passableCells());
        double protectedRatio = e.hazardousBoundaries() == 0 ? 1
                : ratio(e.protectedHazardousBoundaries(), e.hazardousBoundaries());
        double safety = e.passableCells() == 0 ? 0 : 100 * (safeRatio * .70 + protectedRatio * .30);

        double connectedSpace = ratio(e.largestConnectedCells(), e.passableCells());
        double connectedFacilities = ratio(e.connectedFacilities(), e.totalFacilities());
        double connectedEdges = e.territoryEdges() == 0 ? (e.coreChunk() ? 1 : 0)
                : ratio(e.connectedTerritoryEdges(), e.territoryEdges());
        double connectivity = e.passableCells() == 0 ? 0
                : 100 * (connectedSpace * .50 + connectedFacilities * .30 + connectedEdges * .20);
        if (e.coreChunk() && !e.coreConnected()) {
            connectivity = Math.min(39, connectivity);
            limits.add(CivilityLimitReason.CORE_DISCONNECTED);
        } else if (!e.coreChunk() && e.connectedTerritoryEdges() == 0) {
            connectivity = Math.min(39, connectivity);
            limits.add(CivilityLimitReason.TERRITORY_DISCONNECTED);
        }

        CivilizationFactors factors = new CivilizationFactors(building, facilities, safety, connectivity);
        double total=weights.building+weights.facilities+weights.safety+weights.connectivity;
        double uncapped = total<=0?0:(factors.building()*weights.building+factors.facilities()*weights.facilities
                +factors.safety()*weights.safety+factors.connectivity()*weights.connectivity)/total;
        double target = uncapped;
        if (factors.building() < 20) { limits.add(CivilityLimitReason.LOW_BUILDING); target = Math.min(target, 39); }
        if (factors.connectivity() < 20) { limits.add(CivilityLimitReason.LOW_CONNECTIVITY); target = Math.min(target, 39); }
        if (factors.facilities() < 20) { limits.add(CivilityLimitReason.LOW_FACILITIES); target = Math.min(target, 59); }
        if (factors.safety() < 20) { limits.add(CivilityLimitReason.LOW_SAFETY); target = Math.min(target, 59); }
        return new CivilityScore(factors, uncapped, target, limits);
    }

    public record Weights(double building,double facilities,double safety,double connectivity){
        public Weights{building=Math.max(0,building);facilities=Math.max(0,facilities);safety=Math.max(0,safety);connectivity=Math.max(0,connectivity);}
    }

    private static double ratio(int numerator, int denominator) { return denominator <= 0 ? 0 : Math.min(1, numerator / (double)denominator); }
    private static double saturating(int value, int fullAt) { return Math.min(1, value / (double)fullAt); }
    private CivilityScoringRules() {}
}
