package com.seaquake6324.civitas.infrastructure.population;

import com.seaquake6324.civitas.domain.building.BuildingStatus;
import com.seaquake6324.civitas.domain.population.AgePhysicalRules;
import com.seaquake6324.civitas.domain.population.CitizenRecord;
import com.seaquake6324.civitas.infrastructure.entity.CitizenEntity;
import com.seaquake6324.civitas.infrastructure.entity.MaterializationLeaseManager;
import com.seaquake6324.civitas.infrastructure.network.OpenCitizenInfoPayload;
import com.seaquake6324.civitas.infrastructure.persistence.BuildingSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import com.seaquake6324.civitas.infrastructure.security.GuardAssignmentManager;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Main-thread adapter that validates proximity and sends one server-derived resident snapshot. */
public final class CitizenInfoSync {
    private static final double MAX_DISTANCE_SQUARED = 64.0;

    public static boolean refresh(ServerPlayer player, UUID citizenId) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        var lease = MaterializationLeaseManager.lease(citizenId).orElse(null);
        if (lease == null || !(level.getEntity(lease.entityId()) instanceof CitizenEntity entity)) return false;
        return send(player, entity);
    }

    public static boolean send(ServerPlayer player, CitizenEntity entity) {
        if (!(player.level() instanceof ServerLevel level) || player.distanceToSqr(entity) > MAX_DISTANCE_SQUARED) return false;
        CitizenRecord record = entity.citizenId()
                .flatMap(PopulationSavedData.get(level.getServer())::citizen)
                .orElse(null);
        if (record == null) return false;
        var age = PopulationAgingManager.rules().evaluate(record.ageTicks());
        var physical = new AgePhysicalRules().forStage(age.stage());
        var building = record.residenceId() == null
                ? null
                : BuildingSavedData.get(level.getServer()).byId(record.residenceId()).orElse(null);
        String residence = building == null ? "none" : building.status() == BuildingStatus.VALID ? "valid" : "stale";
        var needs = record.needs();
        var guard = GuardAssignmentManager.view(player, entity, record);
        PacketDistributor.sendToPlayer(player, new OpenCitizenInfoPayload(
                record.id(), record.displayName(), record.race().name(), record.gender().name(), age.stage().name(),
                (int) Math.min(Integer.MAX_VALUE, age.completedYears()), record.health(), (float) physical.bodyScale(),
                (float) physical.maxHealth(), (float) physical.movementSpeed(), (float) physical.attackDamage(),
                record.profession(), record.runtimeState().name(), needs.food(), needs.safety(), needs.rest(), needs.social(),
                residence, MarriageInteractionManager.action(player, record), ReproductionInteractionManager.npcAction(player, record),
                guard.action(), guard.equipment().basicWeapon(), guard.equipment().armorPieces(), guard.equipment().shield(),
                guard.dayWillingness(), guard.nightWillingness(), guard.routes(), record.revision()));
        return true;
    }

    private CitizenInfoSync() {}
}
