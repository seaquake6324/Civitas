package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.domain.population.Gender;
import java.util.*;

/** Pure bounded stable pairing for autonomous NPC reproduction; marriage is deliberately not an input. */
public final class PlanNpcReproductionPairsService {
    public Result plan(Collection<Candidate> candidates,int pairCap){
        if(candidates==null||pairCap<1)throw new IllegalArgumentException("invalid NPC reproduction plan");
        Map<UUID,List<Candidate>>males=new TreeMap<>(),females=new TreeMap<>();int examined=0;
        for(Candidate c:candidates){examined++;if(!c.eligible()||c.cityId()==null)continue;(c.gender()==Gender.MALE?males:females).computeIfAbsent(c.cityId(),ignored->new ArrayList<>()).add(c);}
        List<Pair>pairs=new ArrayList<>();for(UUID city:males.keySet()){List<Candidate>a=males.get(city),b=new ArrayList<>(females.getOrDefault(city,List.of()));a.sort(Comparator.comparing(Candidate::citizenId));b.sort(Comparator.comparing(Candidate::citizenId));int count=Math.min(a.size(),b.size());for(int i=0;i<count&&pairs.size()<pairCap;i++)pairs.add(new Pair(a.get(i),b.get(i)));if(pairs.size()>=pairCap)break;}
        return new Result(pairs,examined,pairs.size()>=pairCap);
    }
    public record Candidate(UUID citizenId,UUID cityId,Gender gender,boolean eligible){public Candidate{Objects.requireNonNull(citizenId);Objects.requireNonNull(gender);}}
    public record Pair(Candidate first,Candidate second){}
    public record Result(List<Pair>pairs,int examined,boolean truncated){public Result{pairs=List.copyOf(pairs);}}
}
