package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.PopulationRepository;
import com.seaquake6324.civitas.domain.population.*;
import java.util.*;

/** Main-thread marriage orchestration. Player consent is bound to the actual player actor. */
public final class MarriageService {
    public ProposalResult propose(PopulationRepository repository, UUID proposalId, UUID householdId,
            MarriageEligibilityRules.Participant proposer, MarriageEligibilityRules.Participant target,
            MarriageEligibilityRules.Settings settings, FamilyMemberRef playerActor, long now, long expiresAt,
            int maximumStoredProposals) {
        MarriageEligibilityRules.Result eligibility=MarriageEligibilityRules.evaluate(proposer,target,settings);
        if(!eligibility.eligible())return new ProposalResult(false,Failure.INELIGIBLE,null,null,eligibility);
        if(!validActor(playerActor,proposer.member(),target.member()))return new ProposalResult(false,Failure.INVALID_ACTOR,null,null,eligibility);
        if(repository.proposal(proposalId).isPresent()||repository.household(householdId).isPresent())return new ProposalResult(false,Failure.DUPLICATE_ID,null,null,eligibility);
        if(repository.activeProposalFor(proposer.member()).isPresent()||repository.activeProposalFor(target.member()).isPresent())return new ProposalResult(false,Failure.ACTIVE_PROPOSAL,null,null,eligibility);
        if(now<repository.nextProposalAt(proposer.member()))return new ProposalResult(false,Failure.COOLDOWN,null,null,eligibility);
        if(repository.proposalCount()>=Math.max(1,maximumStoredProposals))return new ProposalResult(false,Failure.CAPACITY,null,null,eligibility);
        boolean proposerConfirmed=proposer.member().kind()==FamilyMemberRef.Kind.CITIZEN||proposer.member().equals(playerActor);
        boolean targetConfirmed=target.member().kind()==FamilyMemberRef.Kind.CITIZEN||target.member().equals(playerActor);
        MarriageProposal.Status status=proposerConfirmed&&targetConfirmed?MarriageProposal.Status.ACCEPTED:MarriageProposal.Status.PENDING;
        MarriageProposal proposal=new MarriageProposal(proposalId,proposer.cityId(),proposer.member(),target.member(),proposerConfirmed,targetConfirmed,now,expiresAt,status,1);
        Household household=null;
        if(status==MarriageProposal.Status.ACCEPTED){household=formHousehold(repository,proposal,householdId,proposer,target);if(household==null)return new ProposalResult(false,Failure.REVALIDATION_FAILED,null,null,eligibility);}
        repository.putProposal(proposal);
        repository.setNextProposalAt(proposer.member(),now+settings.proposalCooldownTicks());
        return new ProposalResult(true,null,proposal,household,eligibility);
    }

    public ConfirmationResult confirm(PopulationRepository repository,UUID proposalId,FamilyMemberRef playerActor,
            long expectedRevision,UUID householdId,long now,MarriageEligibilityRules.Participant currentProposer,
            MarriageEligibilityRules.Participant currentTarget,MarriageEligibilityRules.Settings settings){
        MarriageProposal current=repository.proposal(proposalId).orElse(null);
        if(current==null)return new ConfirmationResult(false,Failure.MISSING,null,null,null);
        if(current.revision()!=expectedRevision)return new ConfirmationResult(false,Failure.STALE_REVISION,current,null,null);
        if(current.status()!=MarriageProposal.Status.PENDING)return new ConfirmationResult(false,Failure.TERMINAL_STATUS,current,null,null);
        MarriageProposal checked=current.expire(now);
        if(checked.status()==MarriageProposal.Status.EXPIRED){repository.putProposal(checked);return new ConfirmationResult(false,Failure.EXPIRED,checked,null,null);}
        if(playerActor==null||playerActor.kind()!=FamilyMemberRef.Kind.PLAYER||!checked.involves(playerActor))return new ConfirmationResult(false,Failure.INVALID_ACTOR,checked,null,null);
        if(!matches(checked,currentProposer,currentTarget))return new ConfirmationResult(false,Failure.REVALIDATION_FAILED,checked,null,null);
        MarriageEligibilityRules.Result eligibility=MarriageEligibilityRules.evaluate(currentProposer,currentTarget,settings);
        if(!eligibility.eligible())return new ConfirmationResult(false,Failure.INELIGIBLE,checked,null,eligibility);
        if((checked.proposer().equals(playerActor)&&checked.proposerConfirmed())||(checked.target().equals(playerActor)&&checked.targetConfirmed()))return new ConfirmationResult(false,Failure.ALREADY_CONFIRMED,checked,null,eligibility);
        MarriageProposal confirmed=checked.confirm(playerActor);Household household=null;
        if(confirmed.status()==MarriageProposal.Status.ACCEPTED){
            if(repository.household(householdId).isPresent())return new ConfirmationResult(false,Failure.DUPLICATE_ID,checked,null,eligibility);
            household=formHousehold(repository,confirmed,householdId,currentProposer,currentTarget);
            if(household==null)return new ConfirmationResult(false,Failure.REVALIDATION_FAILED,checked,null,eligibility);
        }
        repository.putProposal(confirmed);
        return new ConfirmationResult(true,null,confirmed,household,eligibility);
    }

    public ConfirmationResult decline(PopulationRepository repository,UUID proposalId,FamilyMemberRef playerActor,long expectedRevision,long now){
        MarriageProposal current=repository.proposal(proposalId).orElse(null);
        if(current==null)return new ConfirmationResult(false,Failure.MISSING,null,null,null);
        if(current.revision()!=expectedRevision)return new ConfirmationResult(false,Failure.STALE_REVISION,current,null,null);
        if(current.status()!=MarriageProposal.Status.PENDING)return new ConfirmationResult(false,Failure.TERMINAL_STATUS,current,null,null);
        MarriageProposal checked=current.expire(now);
        if(checked.status()==MarriageProposal.Status.EXPIRED){repository.putProposal(checked);return new ConfirmationResult(false,Failure.EXPIRED,checked,null,null);}
        if(playerActor==null||playerActor.kind()!=FamilyMemberRef.Kind.PLAYER||!checked.involves(playerActor))return new ConfirmationResult(false,Failure.INVALID_ACTOR,checked,null,null);
        MarriageProposal declined=checked.decline(playerActor);repository.putProposal(declined);return new ConfirmationResult(true,null,declined,null,null);
    }

    private static boolean validActor(FamilyMemberRef actor,FamilyMemberRef proposer,FamilyMemberRef target){
        boolean hasPlayer=proposer.kind()==FamilyMemberRef.Kind.PLAYER||target.kind()==FamilyMemberRef.Kind.PLAYER;
        return hasPlayer?actor!=null&&actor.kind()==FamilyMemberRef.Kind.PLAYER&&(actor.equals(proposer)||actor.equals(target)):actor==null;
    }
    private static boolean matches(MarriageProposal proposal,MarriageEligibilityRules.Participant proposer,MarriageEligibilityRules.Participant target){return proposal.proposer().equals(proposer.member())&&proposal.target().equals(target.member())&&proposal.cityId().equals(proposer.cityId())&&proposal.cityId().equals(target.cityId());}
    private static Household formHousehold(PopulationRepository repository,MarriageProposal proposal,UUID householdId,MarriageEligibilityRules.Participant proposer,MarriageEligibilityRules.Participant target){if(repository.householdForPartner(proposal.proposer()).isPresent()||repository.householdForPartner(proposal.target()).isPresent())return null;UUID residence=sharedResidence(repository,proposal.proposer(),proposal.target());Household household=new Household(householdId,proposal.cityId(),Set.of(proposal.proposer(),proposal.target()),Set.of(),residence,0,0,1);Map<UUID,Long>expected=new HashMap<>();if(proposer.member().kind()==FamilyMemberRef.Kind.CITIZEN)expected.put(proposer.member().id(),proposer.sourceRevision());if(target.member().kind()==FamilyMemberRef.Kind.CITIZEN)expected.put(target.member().id(),target.sourceRevision());return repository.createHousehold(household,expected)?household:null;}
    private static UUID sharedResidence(PopulationRepository repository,FamilyMemberRef a,FamilyMemberRef b){if(a.kind()!=FamilyMemberRef.Kind.CITIZEN||b.kind()!=FamilyMemberRef.Kind.CITIZEN)return null;UUID first=repository.citizen(a.id()).map(CitizenRecord::residenceId).orElse(null),second=repository.citizen(b.id()).map(CitizenRecord::residenceId).orElse(null);return Objects.equals(first,second)?first:null;}
    public enum Failure{INELIGIBLE,COOLDOWN,INVALID_ACTOR,DUPLICATE_ID,ACTIVE_PROPOSAL,CAPACITY,MISSING,STALE_REVISION,EXPIRED,TERMINAL_STATUS,ALREADY_CONFIRMED,REVALIDATION_FAILED}
    public record ProposalResult(boolean success,Failure failure,MarriageProposal proposal,Household household,MarriageEligibilityRules.Result eligibility){}
    public record ConfirmationResult(boolean success,Failure failure,MarriageProposal proposal,Household household,MarriageEligibilityRules.Result eligibility){}
}
