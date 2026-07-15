package com.seaquake6324.civitas.infrastructure.security;

import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.security.InfiltrationPlan;
import com.seaquake6324.civitas.domain.security.InfiltrationSelectionRules;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.BuildingSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.ThreatSavedData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Main-thread bounded adapter that turns current world evidence into one persisted event-site decision. */
public final class InfiltrationPlanner {
    private static final InfiltrationCellScanner SCANNER = new InfiltrationCellScanner();
    private static final InfiltrationSelectionRules RULES = new InfiltrationSelectionRules();
    private static long attempts, examinedCells, worldSamples, candidates, selected, noCandidate,
            truncatedPlans, staleRevalidations, unloadedCells, totalMicros, maxMicros;
    private static String lastReason = "not_run";

    public static Result plan(MinecraftServer server, City city, long now) {
        long started = System.nanoTime(); attempts++;
        ServerLevel level = level(server, city.dimension());
        if (level == null) return finish(started, null, "missing_dimension");
        var page = ThreatSavedData.get(server).securityCellPage(city.id(), CivitasConfig.INFILTRATION_EVENT_CELL_CAP.get());
        var cells = page.cells().stream().sorted(Comparator.comparingDouble(com.seaquake6324.civitas.domain.security.SecurityCell::diagnosticRisk).reversed()
                .thenComparingLong(com.seaquake6324.civitas.domain.security.SecurityCell::chunk)).toList();
        ArrayList<InfiltrationSelectionRules.Evidence> evidence = new ArrayList<>();
        int samples = 0, examined = 0, cap = CivitasConfig.INFILTRATION_EVENT_CANDIDATE_CAP.get();
        boolean truncated = page.truncated();
        BuildingSavedData buildings = BuildingSavedData.get(server);
        ThreatSavedData threat = ThreatSavedData.get(server);
        for (var cell : cells) {
            if (evidence.size() >= cap) { truncated = true; break; }
            examined++; examinedCells++;
            var buildingPage = buildings.recordsInChunk(city.id(), city.dimension(), cell.chunk(), CivitasConfig.SECURITY_BUILDING_RECORD_LIMIT_PER_CELL.get());
            var coverage = threat.coverageEvidence(city.id(), cell.chunk(), now, CivitasConfig.PATROL_COVERAGE_MEMORY_TICKS.get());
            var scan = SCANNER.scan(level, city, cell.chunk(), buildingPage.records(), !buildingPage.truncated(),
                    CivitasConfig.INFILTRATION_WORLD_SAMPLES_PER_CELL.get(), Math.min(CivitasConfig.INFILTRATION_CANDIDATES_PER_CELL.get(), cap - evidence.size()),
                    CivitasConfig.INFILTRATION_GUARDED_THRESHOLD.get(), coverage.patrol(), coverage.visibleGuard());
            samples += scan.worldSamples(); worldSamples += scan.worldSamples();
            if (!scan.infiltrationEvidenceLinked() && !scan.truncated()) unloadedCells++;
            truncated |= buildingPage.truncated() || scan.truncated();
            for (var observed : scan.candidates()) evidence.add(new InfiltrationSelectionRules.Evidence(observed.source(), observed.position(),
                    observed.chunk(), cell.diagnosticRisk(), observed.darkness(), coverage.patrol(), coverage.visibleGuard()));
        }
        candidates += evidence.size();
        if (evidence.isEmpty()) { noCandidate++; return finish(started, null, "no_loaded_legal_source"); }
        InfiltrationPlan plan = RULES.select(city.id(), city.revision(), evidence, weights(), level.getRandom().nextDouble(), now,
                examined, samples, truncated, cap);
        selected++; if (plan.truncated()) truncatedPlans++;
        return finish(started, plan, "selected_" + plan.selected().source().name().toLowerCase(java.util.Locale.ROOT));
    }

    public static boolean revalidate(MinecraftServer server, City city, InfiltrationPlan plan) {
        if (plan == null || !plan.cityId().equals(city.id()) || plan.cityRevision() != city.revision()) { staleRevalidations++; lastReason = "stale_city_revision"; return false; }
        ServerLevel level = level(server, city.dimension());
        if (level == null) { staleRevalidations++; lastReason = "missing_dimension"; return false; }
        var selected = plan.selected();
        var page = BuildingSavedData.get(server).recordsInChunk(city.id(), city.dimension(), selected.chunk(), CivitasConfig.SECURITY_BUILDING_RECORD_LIMIT_PER_CELL.get());
        boolean valid = !page.truncated() && SCANNER.revalidate(level, selected.position(), page.records());
        if (!valid) { staleRevalidations++; lastReason = page.truncated() ? "building_evidence_truncated" : "selected_source_stale"; }
        return valid;
    }

    private static Result finish(long started, InfiltrationPlan plan, String reason) {
        long micros = Math.max(0, (System.nanoTime() - started) / 1_000); totalMicros += micros; maxMicros = Math.max(maxMicros, micros); lastReason = reason;
        return new Result(Optional.ofNullable(plan), reason);
    }
    private static ServerLevel level(MinecraftServer server, String dimension) { for (ServerLevel level : server.getAllLevels()) if (level.dimension().identifier().toString().equals(dimension)) return level; return null; }
    private static InfiltrationSelectionRules.Weights weights() { return new InfiltrationSelectionRules.Weights(
            CivitasConfig.INFILTRATION_UNCONTROLLED_ENTRANCE_WEIGHT.get(), CivitasConfig.INFILTRATION_WALL_GAP_WEIGHT.get(),
            CivitasConfig.INFILTRATION_CAVE_WEIGHT.get(), CivitasConfig.INFILTRATION_UNDERGROUND_DARKNESS_WEIGHT.get(),
            CivitasConfig.INFILTRATION_DISGUISED_ENTRY_WEIGHT.get(), CivitasConfig.INFILTRATION_SECURITY_COMPONENT_WEIGHT.get(),
            CivitasConfig.INFILTRATION_DARKNESS_COMPONENT_WEIGHT.get(), CivitasConfig.INFILTRATION_UNGUARDED_COMPONENT_WEIGHT.get()); }
    public static Metrics metrics() { return new Metrics(attempts, examinedCells, worldSamples, candidates, selected, noCandidate,
            truncatedPlans, staleRevalidations, unloadedCells, attempts == 0 ? 0 : totalMicros / attempts, maxMicros, lastReason); }
    public record Result(Optional<InfiltrationPlan> plan, String reason) {}
    public record Metrics(long attempts,long examinedCells,long worldSamples,long candidates,long selected,long noCandidate,long truncatedPlans,
            long staleRevalidations,long unloadedCells,long averageMicros,long maxMicros,String lastReason) {}
    private InfiltrationPlanner() {}
}
