package com.seaquake6324.civitas.infrastructure.security;

import com.seaquake6324.civitas.domain.security.InvasionCommitment;
import com.seaquake6324.civitas.infrastructure.border.InvasionMobIndex;
import com.seaquake6324.civitas.infrastructure.border.InvasionMobMarker;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.entity.CitizenEntity;
import com.seaquake6324.civitas.infrastructure.entity.MaterializationLeaseManager;
import com.seaquake6324.civitas.infrastructure.persistence.CitizenEquipmentSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.ThreatSavedData;
import java.util.Comparator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/** Bounded visible-world targeting adapter over the persisted invasion roster. */
public final class VisibleInvasionCombatManager {
    private static long examined, assigned, noCommitment, noTarget; private static String lastReason="not_run";
    public static boolean canGuardEngage(CitizenEntity guard,Mob enemy){if(!(guard.level() instanceof ServerLevel level)||!InvasionMobMarker.active(enemy))return false;var citizen=guard.citizenId().orElse(null);var invasion=InvasionMobMarker.invasionId(enemy).orElse(null);if(citizen==null||invasion==null)return false;InvasionCommitment commitment=ThreatSavedData.get(level.getServer()).invasionCommitment(invasion).orElse(null);if(commitment==null||commitment.participants().stream().noneMatch(p->p.citizenId().equals(citizen)&&p.role()==InvasionCommitment.Role.GUARD))return false;return CitizenEquipmentSavedData.get(level.getServer()).guardEquipment(citizen,level.registryAccess()).basicWeapon();}
    public static void tick(MinecraftServer server){for(Mob mob:InvasionMobIndex.mobBatch(CivitasConfig.VISIBLE_INVASION_MOBS_PER_TICK.get())){examined++;if(!(mob.level() instanceof ServerLevel level))continue;var invasion=InvasionMobMarker.invasionId(mob).orElse(null);InvasionCommitment commitment=invasion==null?null:ThreatSavedData.get(server).invasionCommitment(invasion).orElse(null);if(commitment==null){noCommitment++;lastReason="missing_commitment";continue;}CitizenEntity target=mob.getTarget() instanceof CitizenEntity current&&current.isAlive()&&current.distanceToSqr(mob)<=radiusSquared()?current:commitment.participants().stream().sorted(Comparator.<InvasionCommitment.Participant>comparingInt(p->p.role()==InvasionCommitment.Role.GUARD?0:1).thenComparingInt(InvasionCommitment.Participant::distanceBlocks)).map(p->entity(level,p.citizenId())).filter(java.util.Objects::nonNull).filter(CitizenEntity::isAlive).filter(c->c.distanceToSqr(mob)<=radiusSquared()).findFirst().orElse(null);if(target==null){noTarget++;lastReason="no_loaded_participant";}else{mob.setTarget(target);if(canGuardEngage(target,mob))target.setTarget(mob);assigned++;lastReason="target_assigned";}}}
    private static CitizenEntity entity(ServerLevel level,java.util.UUID citizen){var lease=MaterializationLeaseManager.lease(citizen).orElse(null);if(lease==null)return null;return level.getEntity(lease.entityId())instanceof CitizenEntity entity?entity:null;}
    private static double radiusSquared(){double radius=CivitasConfig.VISIBLE_INVASION_TARGET_RADIUS.get();return radius*radius;}
    public static Metrics metrics(){return new Metrics(examined,assigned,noCommitment,noTarget,lastReason);}public record Metrics(long examined,long assigned,long noCommitment,long noTarget,String lastReason){}
    private VisibleInvasionCombatManager(){}
}
