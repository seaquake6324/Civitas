package com.seaquake6324.civitas.infrastructure.population;

import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.entity.CitizenEntity;
import com.seaquake6324.civitas.infrastructure.persistence.CitizenEquipmentSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Main-thread, bounded bridge that turns persisted equipment into real drops after permanent death. */
public final class CitizenEquipmentDeathDropManager {
    private static MinecraftServer owner;
    private static UUID cursor;
    private static long examined, droppedSnapshots, droppedStacks, deferredChunks, missingLocations, decodeEmpty;

    public static void tick(MinecraftServer server) {
        if (owner != server) reset(server);
        CitizenEquipmentSavedData equipment = CitizenEquipmentSavedData.get(server);
        PopulationSavedData population = PopulationSavedData.get(server);
        if (equipment.readOnly() || population.readOnly()) return;
        var batch = equipment.snapshotBatch(cursor, CivitasConfig.NPC_EQUIPMENT_DEATH_DROPS_PER_TICK.get());
        cursor = batch.nextCursor();
        for (var snapshot : batch.records()) {
            examined++;
            var citizen = population.citizen(snapshot.citizenId()).orElse(null);
            if (citizen == null || citizen.alive()) continue;
            var location = population.location(snapshot.citizenId()).orElse(null);
            if (location == null) { missingLocations++; continue; }
            ServerLevel level = level(server, location.dimension());
            BlockPos pos = BlockPos.of(location.position());
            if (level == null || !level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) { deferredChunks++; continue; }
            var stacks = equipment.decodedItems(snapshot.citizenId(), level.registryAccess());
            if (stacks.isEmpty()) decodeEmpty++;
            for (ItemStack stack : stacks) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, stack));
                droppedStacks++;
            }
            equipment.remove(snapshot.citizenId());
            droppedSnapshots++;
        }
    }

    public static void dropVisible(CitizenEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        UUID citizenId = entity.citizenId().orElse(null);
        if (citizenId == null) return;
        CitizenEquipmentSavedData equipment = CitizenEquipmentSavedData.get(level.getServer());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                entity.spawnAtLocation(level, stack.copy());
                entity.setItemSlot(slot, ItemStack.EMPTY);
                droppedStacks++;
            }
        }
        equipment.remove(citizenId);
        droppedSnapshots++;
    }

    /** Vanilla death already owns the visible entity drops; only retire its virtual snapshot. */
    public static void retireSnapshot(MinecraftServer server, UUID citizenId) {
        CitizenEquipmentSavedData.get(server).remove(citizenId);
    }

    private static ServerLevel level(MinecraftServer server, String dimension) {
        for (ServerLevel level : server.getAllLevels()) if (level.dimension().identifier().toString().equals(dimension)) return level;
        return null;
    }

    private static void reset(MinecraftServer server) {
        owner = server; cursor = null;
        examined = droppedSnapshots = droppedStacks = deferredChunks = missingLocations = decodeEmpty = 0;
    }

    public static Metrics metrics() { return new Metrics(examined, droppedSnapshots, droppedStacks, deferredChunks, missingLocations, decodeEmpty); }
    public record Metrics(long examined, long droppedSnapshots, long droppedStacks, long deferredChunks, long missingLocations, long decodeEmpty) {}
    private CitizenEquipmentDeathDropManager() {}
}
