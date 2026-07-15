package com.seaquake6324.civitas.domain.territory;
import com.seaquake6324.civitas.domain.City;
import java.util.Comparator;
import java.util.Map;
import java.util.OptionalLong;
public final class TerritoryRetraction {
    public record Health(double civility,double activity){}
    public static OptionalLong select(City city,Map<Long,Health> health){
        return city.territory().stream().filter(c->!city.heartland().contains(c)&&c!=city.coreChunk())
            .filter(c->TerritoryTopology.borderEdges(city.territory(),c).size()>0)
            .filter(c->city.territoryStates().get(c)!=null&&city.territoryStates().get(c).neglectStage()==NeglectStage.RETRACTABLE)
            .filter(c->TerritoryTopology.removable(city.territory(),c,city.coreChunk()))
            .min(Comparator.<Long>comparingLong(c->city.territoryStates().get(c).neglectStartedAt())
                .thenComparingDouble(c->{Health h=health.getOrDefault(c,new Health(0,0));return h.civility+h.activity;})
                .thenComparing(Comparator.comparingLong((Long c)->distance(c,city.coreChunk())).reversed()))
            .map(OptionalLong::of).orElseGet(OptionalLong::empty);
    }
    private static long distance(long a,long b){var x=com.seaquake6324.civitas.domain.ChunkCoordinate.unpack(a);var y=com.seaquake6324.civitas.domain.ChunkCoordinate.unpack(b);return Math.abs((long)x.x()-y.x())+Math.abs((long)x.z()-y.z());}
    private TerritoryRetraction(){}
}
