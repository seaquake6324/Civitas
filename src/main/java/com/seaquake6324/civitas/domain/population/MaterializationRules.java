package com.seaquake6324.civitas.domain.population;

/** Pure hysteresis rules for switching between virtual and visible citizen simulation. */
public final class MaterializationRules {
    private final int materializeDistance;
    private final int prewarmDistance;
    private final int exitDistance;

    public MaterializationRules(int materializeDistance, int prewarmDistance, int exitDistance) {
        if (materializeDistance < 1 || prewarmDistance < materializeDistance || exitDistance < prewarmDistance)
            throw new IllegalArgumentException("invalid materialization distances");
        this.materializeDistance = materializeDistance;
        this.prewarmDistance = prewarmDistance;
        this.exitDistance = exitDistance;
    }

    public Decision evaluate(CitizenRuntimeState current, double nearestPlayerDistance,
            boolean hasValidNode, boolean entityPresent, boolean entityLocked) {
        if (entityPresent && entityLocked) return new Decision(CitizenRuntimeState.LOCKED, "entity_locked");
        if (current == CitizenRuntimeState.LOCKED && entityPresent)
            return new Decision(CitizenRuntimeState.MATERIALIZED, "lock_released");
        if (entityPresent && nearestPlayerDistance > exitDistance)
            return new Decision(CitizenRuntimeState.VIRTUAL, "outside_exit_radius");
        if (entityPresent) return new Decision(CitizenRuntimeState.MATERIALIZED, "entity_present");
        if (!hasValidNode) return new Decision(CitizenRuntimeState.VIRTUAL, "no_valid_node");
        if (nearestPlayerDistance <= materializeDistance)
            return new Decision(CitizenRuntimeState.MATERIALIZED, "inside_materialize_radius");
        if (nearestPlayerDistance <= prewarmDistance)
            return new Decision(CitizenRuntimeState.PREWARMING, "inside_prewarm_radius");
        return new Decision(CitizenRuntimeState.VIRTUAL, "outside_interest_radius");
    }

    public record Decision(CitizenRuntimeState state, String reason) {}
}
