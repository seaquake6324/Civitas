package com.seaquake6324.civitas.domain.population;

import java.util.Set;
import java.util.UUID;

/** Persistent in-progress pregnancy; birth outcome remains a main-thread commit. */
public record PregnancyRecord(UUID id, UUID cityId, Set<FamilyMemberRef> parents,
                              FamilyMemberRef gestationalParent, long conceivedAt, long dueAt,
                              boolean infidelity, double conceptionWillingness, ReproductionRules.Result evaluation, long revision) {
    public PregnancyRecord(UUID id,UUID cityId,Set<FamilyMemberRef>parents,FamilyMemberRef gestationalParent,long conceivedAt,long dueAt,boolean infidelity,double conceptionWillingness,long revision){this(id,cityId,parents,gestationalParent,conceivedAt,dueAt,infidelity,conceptionWillingness,null,revision);}
    public PregnancyRecord {
        if (id == null || cityId == null || parents == null || gestationalParent == null)
            throw new IllegalArgumentException("Missing pregnancy identity");
        parents = Set.copyOf(parents);
        if (parents.size() != 2 || !parents.contains(gestationalParent))
            throw new IllegalArgumentException("Pregnancy requires two distinct parents and one carrier");
        if (conceivedAt < 0 || dueAt <= conceivedAt || revision < 0
                || !Double.isFinite(conceptionWillingness) || conceptionWillingness < 0 || conceptionWillingness > 100)
            throw new IllegalArgumentException("Invalid pregnancy timing or score");
        if(evaluation!=null&&Double.compare(evaluation.effectiveWillingness(),conceptionWillingness)!=0)throw new IllegalArgumentException("Pregnancy evaluation score mismatch");
    }
}
