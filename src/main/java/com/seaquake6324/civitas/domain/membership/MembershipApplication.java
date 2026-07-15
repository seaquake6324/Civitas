package com.seaquake6324.civitas.domain.membership;

import java.util.UUID;

public record MembershipApplication(UUID playerId, long submittedAt, Status status) {
    public enum Status { PENDING, APPROVED, REJECTED }
    public MembershipApplication { if (playerId == null || status == null || submittedAt < 0) throw new IllegalArgumentException(); }
}
