package com.seaquake6324.civitas.domain.population;

import java.util.Set;

/** Records only whether conception is outside an active monogamous marriage. */
public final class InfidelityRules {
    public boolean isInfidelity(FamilyMemberRef first,FamilyMemberRef second,
                                Set<FamilyMemberRef> firstSpouses,Set<FamilyMemberRef> secondSpouses){
        if(first==null||second==null)return false;
        boolean marriedToEachOther=firstSpouses.contains(second)&&secondSpouses.contains(first);
        if(marriedToEachOther)return false;
        return !firstSpouses.isEmpty()||!secondSpouses.isEmpty();
    }
}
