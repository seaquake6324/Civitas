package com.seaquake6324.civitas.domain.population;

import java.util.UUID;

/** Persistent, revisioned consent state. Player consent is never inferred from presence or inactivity. */
public record MarriageProposal(UUID id,UUID cityId,FamilyMemberRef proposer,FamilyMemberRef target,
        boolean proposerConfirmed,boolean targetConfirmed,long createdAt,long expiresAt,Status status,long revision){
    public enum Status{PENDING,ACCEPTED,DECLINED,EXPIRED,CANCELLED}
    public MarriageProposal{if(id==null||cityId==null||proposer==null||target==null||proposer.equals(target))throw new IllegalArgumentException("invalid marriage proposal");if(createdAt<0||expiresAt<=createdAt||revision<1)throw new IllegalArgumentException("invalid proposal timing");if(status==null)throw new IllegalArgumentException("missing proposal status");if((status==Status.ACCEPTED)!=(proposerConfirmed&&targetConfirmed))throw new IllegalArgumentException("proposal consent does not match status");}
    public boolean involves(FamilyMemberRef member){return proposer.equals(member)||target.equals(member);}
    public MarriageProposal confirm(FamilyMemberRef member){if(status!=Status.PENDING||!involves(member))return this;boolean a=proposerConfirmed||proposer.equals(member),b=targetConfirmed||target.equals(member);return new MarriageProposal(id,cityId,proposer,target,a,b,createdAt,expiresAt,a&&b?Status.ACCEPTED:Status.PENDING,revision+1);}
    public MarriageProposal decline(FamilyMemberRef member){return status==Status.PENDING&&involves(member)?new MarriageProposal(id,cityId,proposer,target,proposerConfirmed,targetConfirmed,createdAt,expiresAt,Status.DECLINED,revision+1):this;}
    public MarriageProposal expire(long now){return status==Status.PENDING&&now>=expiresAt?new MarriageProposal(id,cityId,proposer,target,proposerConfirmed,targetConfirmed,createdAt,expiresAt,Status.EXPIRED,revision+1):this;}
}
