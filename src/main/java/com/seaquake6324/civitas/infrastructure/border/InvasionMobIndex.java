package com.seaquake6324.civitas.infrastructure.border;

import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/** Main-thread index of loaded invasion mobs; overflow is explicit and fails closed. */
public final class InvasionMobIndex{
    private static final Map<UUID,LinkedHashSet<Mob>>BY_INVASION=new HashMap<>();
    private static int retained;private static boolean overflow;private static long registrations,rejections,queries,staleRemoved,releases;
    public static void register(Mob mob){InvasionMobMarker.invasionId(mob).ifPresent(id->{LinkedHashSet<Mob>mobs=BY_INVASION.computeIfAbsent(id,ignored->new LinkedHashSet<>());if(mobs.contains(mob))return;if(retained>=CivitasConfig.THREAT_MOB_INDEX_CAP.get()){overflow=true;rejections++;return;}mobs.add(mob);retained++;registrations++;});}
    public static void unregister(Mob mob){InvasionMobMarker.invasionId(mob).ifPresent(id->{LinkedHashSet<Mob>mobs=BY_INVASION.get(id);if(mobs!=null&&mobs.remove(mob)){retained--;if(mobs.isEmpty())BY_INVASION.remove(id);}});}
    public static Count count(ServerLevel level,UUID invasion){queries++;LinkedHashSet<Mob>mobs=BY_INVASION.get(invasion);if(mobs==null)return new Count(0,overflow);int count=0;for(Iterator<Mob>it=mobs.iterator();it.hasNext();){Mob mob=it.next();if(mob.isRemoved()||!InvasionMobMarker.active(mob)){it.remove();retained--;staleRemoved++;continue;}if(mob.level()==level&&mob.isAlive())count++;}if(mobs.isEmpty())BY_INVASION.remove(invasion);return new Count(count,overflow);}
    public static int release(ServerLevel level,UUID invasion){LinkedHashSet<Mob>mobs=BY_INVASION.get(invasion);if(mobs==null)return 0;int count=0;for(Iterator<Mob>it=mobs.iterator();it.hasNext();){Mob mob=it.next();if(mob.level()==level){InvasionMobMarker.release(mob);it.remove();retained--;count++;}}if(mobs.isEmpty())BY_INVASION.remove(invasion);releases+=count;return count;}
    public static List<Mob>mobBatch(int limit){int cap=Math.max(0,limit);if(cap==0)return List.of();ArrayList<Mob>out=new ArrayList<>(Math.min(cap,retained));for(var mobs:BY_INVASION.values())for(Mob mob:mobs){if(out.size()>=cap)return List.copyOf(out);if(!mob.isRemoved()&&mob.isAlive()&&InvasionMobMarker.active(mob))out.add(mob);}return List.copyOf(out);}
    public static void clear(){BY_INVASION.clear();retained=0;overflow=false;}
    public static Metrics metrics(){return new Metrics(retained,BY_INVASION.size(),overflow,registrations,rejections,queries,staleRemoved,releases);}
    public record Count(int loadedAlive,boolean incomplete){}
    public record Metrics(int retained,int invasions,boolean overflow,long registrations,long rejections,long queries,long staleRemoved,long releases){}
    private InvasionMobIndex(){}
}
