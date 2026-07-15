package com.seaquake6324.civitas.domain.territory;

import com.seaquake6324.civitas.domain.City;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ExpansionEligibility {
    public enum Failure { NOT_MANAGER, COOLDOWN, DIMENSION_DISABLED, TARGET_UNLOADED, ALREADY_CLAIMED,
        BUFFER_CONFLICT, NOT_ADJACENT, INVALID_SOURCE, SOURCE_NOT_BORDER, SOURCE_NOT_DEVELOPED,
        CORE_CIVILITY_LOW, DEVELOPED_COVERAGE_LOW, ACTIVE_COVERAGE_LOW,
        CURRENT_TERRITORY_DISCONNECTED, CORE_OUTSIDE_TERRITORY }
    public record ChunkHealth(double surfaceCivility, double surfaceActivity,
            double undergroundCivility, double undergroundActivity) {
        public boolean developed(double civilityMinimum, double activityMinimum) {
            return surfaceCivility >= civilityMinimum && surfaceActivity >= activityMinimum
                    || undergroundCivility >= civilityMinimum && undergroundActivity >= activityMinimum;
        }
        public double activity() { return Math.max(surfaceActivity, undergroundActivity); }
    }
    public record Rules(double civilityMinimum, double activityMinimum, double developedCoverage,
            double activeCoverage, long cooldownTicks) {}
    public record Context(boolean manager, boolean dimensionAllowed, boolean loaded, boolean claimedByAny,
            boolean bufferConflict, long now, long target, long source, Map<Long, ChunkHealth> health) {}
    public record Result(boolean eligible, List<Failure> failures, int developed, int active, int total) {}

    public static Result evaluate(City city, Context context, Rules rules) {
        List<Failure> failures = new ArrayList<>();
        if (!context.manager) failures.add(Failure.NOT_MANAGER);
        if (!TerritoryTopology.connected(city.territory())) failures.add(Failure.CURRENT_TERRITORY_DISCONNECTED);
        if (!city.territory().contains(city.coreChunk())) failures.add(Failure.CORE_OUTSIDE_TERRITORY);
        if (context.now - city.lastExpansionAt() < rules.cooldownTicks()) failures.add(Failure.COOLDOWN);
        if (!context.dimensionAllowed) failures.add(Failure.DIMENSION_DISABLED);
        if (!context.loaded) failures.add(Failure.TARGET_UNLOADED);
        if (context.claimedByAny) failures.add(Failure.ALREADY_CLAIMED);
        if (context.bufferConflict) failures.add(Failure.BUFFER_CONFLICT);
        if (!TerritoryTopology.touches(city.territory(), context.target)) failures.add(Failure.NOT_ADJACENT);
        if (!city.territory().contains(context.source) || TerritoryTopology.adjacent(context.source,
                directionBetween(context.source, context.target)) != context.target) failures.add(Failure.INVALID_SOURCE);
        if (TerritoryTopology.borderEdges(city.territory(), context.source).isEmpty()) failures.add(Failure.SOURCE_NOT_BORDER);
        ChunkHealth source = context.health.get(context.source);
        if (source == null || !source.developed(rules.civilityMinimum, rules.activityMinimum)) failures.add(Failure.SOURCE_NOT_DEVELOPED);
        ChunkHealth core = context.health.get(city.coreChunk());
        if (core == null || core.surfaceCivility < rules.civilityMinimum) failures.add(Failure.CORE_CIVILITY_LOW);
        int developed=0, active=0;
        for (long chunk : city.territory()) {
            ChunkHealth h=context.health.get(chunk);
            if (h!=null && h.developed(rules.civilityMinimum,rules.activityMinimum)) developed++;
            if (h!=null && h.activity()>=rules.activityMinimum) active++;
        }
        int total=city.territory().size();
        if (total==0 || developed/(double)total < rules.developedCoverage) failures.add(Failure.DEVELOPED_COVERAGE_LOW);
        if (total==0 || active/(double)total < rules.activeCoverage) failures.add(Failure.ACTIVE_COVERAGE_LOW);
        return new Result(failures.isEmpty(), List.copyOf(failures), developed, active, total);
    }

    private static TerritoryTopology.Direction directionBetween(long from, long to) {
        for (var d:TerritoryTopology.Direction.values()) if (TerritoryTopology.adjacent(from,d)==to) return d;
        return TerritoryTopology.Direction.NORTH; // mismatch is rejected by the caller's equality check
    }
    private ExpansionEligibility() {}
}
