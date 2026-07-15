package com.seaquake6324.civitas.infrastructure.security;

import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.population.CitizenRecord;
import com.seaquake6324.civitas.domain.security.InfiltrationPlan;
import com.seaquake6324.civitas.domain.security.InvasionCommitment;
import com.seaquake6324.civitas.domain.security.PatrolAssignment;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.CitizenEquipmentSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.ThreatSavedData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

/** Locks a bounded participant and enemy roster at the formal first-mob commit point. */
public final class InvasionCommitmentManager {
    private static long attempts,created,rejected,citizensExamined,guards,civilians,truncatedRosters;private static String lastReason="not_run";
    public static boolean create(MinecraftServer server,City city,InfiltrationPlan plan,UUID invasionId,int wave,List<UUID>enemyIds,long now){attempts++;ThreatSavedData threat=ThreatSavedData.get(server);PopulationSavedData population=PopulationSavedData.get(server);CitizenEquipmentSavedData equipment=CitizenEquipmentSavedData.get(server);if(threat.readOnly()||population.readOnly()||equipment.readOnly()){rejected++;lastReason="read_only";return false;}int cap=CivitasConfig.BACKGROUND_BATTLE_PARTICIPANT_CAP.get();ArrayList<InvasionCommitment.Participant>participants=new ArrayList<>();HashSet<UUID>included=new HashSet<>();BlockPos site=BlockPos.of(plan.selected().position());for(PatrolAssignment assignment:threat.patrolAssignments(city.id(),CivitasConfig.PATROL_ASSIGNMENT_CITY_CAP.get())){if(participants.size()>=cap)break;CitizenRecord citizen=population.citizen(assignment.citizenId()).orElse(null);if(citizen==null||!citizen.alive()||assignment.status()!=PatrolAssignment.Status.ACTIVE||!equipment.guardEquipment(citizen.id(),server.registryAccess()).basicWeapon())continue;participants.add(new InvasionCommitment.Participant(citizen.id(),InvasionCommitment.Role.GUARD,distance(population,citizen.id(),city.dimension(),site)));included.add(citizen.id());guards++;}var page=population.citizenPage(city.id(),CivitasConfig.BACKGROUND_BATTLE_CITIZEN_SCAN_CAP.get());citizensExamined+=page.examined();if(page.truncated())truncatedRosters++;int radius=CivitasConfig.BACKGROUND_BATTLE_CIVILIAN_RADIUS.get(),civilianCap=CivitasConfig.BACKGROUND_BATTLE_CIVILIAN_CAP.get();var nearby=page.records().stream().filter(CitizenRecord::alive).filter(c->!included.contains(c.id())).map(c->new Candidate(c,distance(population,c.id(),city.dimension(),site))).filter(c->c.distance<=radius).sorted(Comparator.comparingInt(Candidate::distance).thenComparing(c->c.citizen.id())).limit(Math.min(civilianCap,Math.max(0,cap-participants.size()))).toList();for(Candidate candidate:nearby){participants.add(new InvasionCommitment.Participant(candidate.citizen.id(),InvasionCommitment.Role.CIVILIAN,candidate.distance));civilians++;}InvasionCommitment commitment=new InvasionCommitment(invasionId,city.id(),plan.selected().source(),plan.selected().position(),wave,enemyIds,participants,now,now,0,1);if(!threat.createInvasionCommitment(commitment)){rejected++;lastReason="stale_commit";return false;}created++;lastReason="created";return true;}
    public static boolean beginWave(ThreatSavedData threat,UUID invasionId,int wave,List<UUID>enemyIds,long now){var commitment=threat.invasionCommitment(invasionId).orElse(null);if(commitment==null)return true;boolean success=threat.beginCommitmentWave(invasionId,commitment.revision(),wave,enemyIds,now);if(!success){rejected++;lastReason="stale_next_wave";}return success;}
    private static int distance(PopulationSavedData population,UUID citizenId,String dimension,BlockPos site){var location=population.location(citizenId).orElse(null);if(location==null||!dimension.equals(location.dimension()))return CivitasConfig.BACKGROUND_BATTLE_CIVILIAN_RADIUS.get()+1;return (int)Math.min(Integer.MAX_VALUE,Math.round(Math.sqrt(site.distSqr(BlockPos.of(location.position())))));}
    public static Metrics metrics(){return new Metrics(attempts,created,rejected,citizensExamined,guards,civilians,truncatedRosters,lastReason);}public record Metrics(long attempts,long created,long rejected,long citizensExamined,long guards,long civilians,long truncatedRosters,String lastReason){}
    private record Candidate(CitizenRecord citizen,int distance){}
    private InvasionCommitmentManager(){}
}
