package com.seaquake6324.civitas.domain.population;

import java.util.*;

/** Explainable eligibility only; gender is deliberately absent because same-sex marriage is allowed. */
public final class MarriageEligibilityRules{
    public record Participant(FamilyMemberRef member,UUID cityId,boolean alive,boolean adult,boolean partnered,double willingness,long sourceRevision){public Participant{willingness=Math.max(0,Math.min(100,willingness));sourceRevision=Math.max(0,sourceRevision);}}
    public record Settings(double npcWillingnessMinimum,long proposalCooldownTicks){public Settings{npcWillingnessMinimum=Math.max(0,Math.min(100,npcWillingnessMinimum));proposalCooldownTicks=Math.max(0,proposalCooldownTicks);}public Settings(double npcWillingnessMinimum){this(npcWillingnessMinimum,1200);}}
    public enum Failure{SAME_PERSON,DIFFERENT_CITY,DEAD,UNDERAGE,ALREADY_PARTNERED,LOW_WILLINGNESS}
    public record Result(boolean eligible,Set<Failure>failures,double proposerWillingness,double targetWillingness){public Result{failures=Set.copyOf(failures);}}
    public static Result evaluate(Participant proposer,Participant target,Settings settings){EnumSet<Failure>failures=EnumSet.noneOf(Failure.class);if(proposer.member().equals(target.member()))failures.add(Failure.SAME_PERSON);if(proposer.cityId()==null||!proposer.cityId().equals(target.cityId()))failures.add(Failure.DIFFERENT_CITY);if((proposer.member().kind()==FamilyMemberRef.Kind.CITIZEN&&!proposer.alive())||(target.member().kind()==FamilyMemberRef.Kind.CITIZEN&&!target.alive()))failures.add(Failure.DEAD);if((proposer.member().kind()==FamilyMemberRef.Kind.CITIZEN&&!proposer.adult())||(target.member().kind()==FamilyMemberRef.Kind.CITIZEN&&!target.adult()))failures.add(Failure.UNDERAGE);if(proposer.partnered()||target.partnered())failures.add(Failure.ALREADY_PARTNERED);if((proposer.member().kind()==FamilyMemberRef.Kind.CITIZEN&&proposer.willingness()<settings.npcWillingnessMinimum())||(target.member().kind()==FamilyMemberRef.Kind.CITIZEN&&target.willingness()<settings.npcWillingnessMinimum()))failures.add(Failure.LOW_WILLINGNESS);return new Result(failures.isEmpty(),failures,proposer.willingness(),target.willingness());}
    private MarriageEligibilityRules(){}
}
