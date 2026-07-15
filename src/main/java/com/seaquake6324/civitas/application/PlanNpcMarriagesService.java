package com.seaquake6324.civitas.application;
import com.seaquake6324.civitas.domain.population.FamilyMemberRef;import java.util.*;
/** Pure, order-stable and linear NPC pairing. It adds no hidden preference scoring. */
public final class PlanNpcMarriagesService{
 public Result plan(List<Candidate>candidates,double willingnessMinimum,int maximumPairs){Map<UUID,Candidate>waiting=new HashMap<>();List<Pair>pairs=new ArrayList<>();int examined=0,eligible=0,cap=Math.max(0,maximumPairs);for(Candidate candidate:candidates){if(pairs.size()>=cap)break;examined++;if(!candidate.alive()||!candidate.adult()||candidate.partnered()||candidate.activeProposal()||candidate.cityId()==null||candidate.willingness()<willingnessMinimum)continue;eligible++;Candidate first=waiting.remove(candidate.cityId());if(first==null)waiting.put(candidate.cityId(),candidate);else pairs.add(new Pair(first,candidate));}return new Result(pairs,examined,eligible,examined<candidates.size());}
 public record Candidate(FamilyMemberRef member,UUID cityId,boolean alive,boolean adult,boolean partnered,boolean activeProposal,double willingness,long revision){public Candidate{if(member.kind()!=FamilyMemberRef.Kind.CITIZEN)throw new IllegalArgumentException("NPC candidate must be a citizen");willingness=Math.max(0,Math.min(100,willingness));revision=Math.max(0,revision);}}
 public record Pair(Candidate first,Candidate second){}
 public record Result(List<Pair>pairs,int examined,int eligible,boolean truncated){public Result{pairs=List.copyOf(pairs);}}
}
