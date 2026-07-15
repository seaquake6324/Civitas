package com.seaquake6324.civitas.domain.security;
import java.util.*;
/** Minecraft-free authority for one bounded, ordered patrol route. Coverage is inactive until a guard assignment validates it. */
public record PatrolRoute(UUID id,UUID cityId,String dimension,UUID guardPostId,List<Long>nodes,UUID createdBy,Status status,long validatedAt,long revision,String invalidReason){
 public static final int HARD_NODE_CAP=64;
 public PatrolRoute{if(id==null||cityId==null||dimension==null||dimension.isBlank()||guardPostId==null||createdBy==null||status==null)throw new IllegalArgumentException("missing patrol identity");nodes=List.copyOf(nodes);if(nodes.size()<2||nodes.size()>HARD_NODE_CAP)throw new IllegalArgumentException("invalid patrol node count");for(int i=1;i<nodes.size();i++)if(nodes.get(i).equals(nodes.get(i-1)))throw new IllegalArgumentException("duplicate adjacent patrol node");if(validatedAt<0||revision<1)throw new IllegalArgumentException("invalid patrol revision");invalidReason=invalidReason==null?"":invalidReason;if(status==Status.VALID&&!invalidReason.isBlank())throw new IllegalArgumentException("valid patrol has invalid reason");}
 public PatrolRoute stale(String reason){return status==Status.STALE&&Objects.equals(invalidReason,reason)?this:new PatrolRoute(id,cityId,dimension,guardPostId,nodes,createdBy,Status.STALE,validatedAt,revision+1,reason);}
 public enum Status{VALID,STALE,INVALID}
}
