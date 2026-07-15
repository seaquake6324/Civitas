package com.seaquake6324.civitas.domain.population;

import java.util.UUID;

/** Persistent, bounded double-consent request for player-to-player conception. */
public record ReproductionConsent(UUID id, UUID cityId, FamilyMemberRef proposer, FamilyMemberRef target,
                                  long createdAt, long expiresAt, long revision) {
    public ReproductionConsent {
        if (id == null || cityId == null || proposer == null || target == null || proposer.equals(target)
                || proposer.kind() != FamilyMemberRef.Kind.PLAYER || target.kind() != FamilyMemberRef.Kind.PLAYER
                || createdAt < 0 || expiresAt <= createdAt || revision < 1)
            throw new IllegalArgumentException("invalid reproduction consent");
    }
    public boolean involves(FamilyMemberRef member){return proposer.equals(member)||target.equals(member);}
}
