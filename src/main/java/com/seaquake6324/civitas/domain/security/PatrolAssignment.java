package com.seaquake6324.civitas.domain.security;

import java.util.Objects;
import java.util.UUID;

/** Persistent authority for one citizen assigned to one route and fixed shift. */
public record PatrolAssignment(UUID id, UUID cityId, UUID routeId, UUID guardPostId, UUID citizenId,
        GuardShift shift, boolean forced, GuardWillingness willingness, Status status,
        int nodeIndex, boolean forward, long assignedAt, long lastProgressAt, long lastCoverageAt,
        long revision, String inactiveReason) {
    public PatrolAssignment {
        Objects.requireNonNull(id);Objects.requireNonNull(cityId);Objects.requireNonNull(routeId);
        Objects.requireNonNull(guardPostId);Objects.requireNonNull(citizenId);Objects.requireNonNull(shift);
        Objects.requireNonNull(willingness);Objects.requireNonNull(status);
        if(nodeIndex<0||assignedAt<0||lastProgressAt<0||lastCoverageAt<0||revision<1)throw new IllegalArgumentException("invalid patrol assignment");
        inactiveReason=inactiveReason==null?"":inactiveReason;
        if(status==Status.ACTIVE&&!inactiveReason.isBlank())throw new IllegalArgumentException("active assignment has inactive reason");
    }

    public PatrolAssignment withState(Status next, String reason, int nextNode, boolean nextForward, long progressAt, long coverageAt) {
        String why=next==Status.ACTIVE?"":Objects.requireNonNullElse(reason,"inactive");
        if(next==status&&nextNode==nodeIndex&&nextForward==forward&&progressAt==lastProgressAt&&coverageAt==lastCoverageAt&&why.equals(inactiveReason))return this;
        return new PatrolAssignment(id,cityId,routeId,guardPostId,citizenId,shift,forced,willingness,next,
                Math.max(0,nextNode),nextForward,assignedAt,Math.max(lastProgressAt,progressAt),Math.max(lastCoverageAt,coverageAt),revision+1,why);
    }

    public enum Status { ACTIVE, OFF_SHIFT, UNEQUIPPED, ROUTE_STALE, GUARD_MISSING }
}
