package com.seaquake6324.civitas.infrastructure.population;

import com.seaquake6324.civitas.domain.building.BuildingRecord;
import com.seaquake6324.civitas.application.TransitionCitizenRuntimeService;
import com.seaquake6324.civitas.domain.building.BuildingStatus;
import com.seaquake6324.civitas.domain.population.CitizenRecord;
import com.seaquake6324.civitas.domain.population.CitizenRuntimeState;
import com.seaquake6324.civitas.domain.population.MaterializationRules;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.entity.CitizenEntity;
import com.seaquake6324.civitas.infrastructure.entity.MaterializationLeaseManager;
import com.seaquake6324.civitas.infrastructure.persistence.BuildingSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.registry.CivitasEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

/** Main-thread, bounded bridge between persistent citizens and their visible entities. */
public final class CitizenMaterializationManager {
    private static final TransitionCitizenRuntimeService TRANSITIONS=new TransitionCitizenRuntimeService();
    private static final Map<UUID, Long> PREWARMING = new HashMap<>();
    private static MinecraftServer owner;
    private static UUID cursor;
    private static long decisions, spawned, virtualized, rejectedCap, rejectedPrewarm, rejectedNode, staleResults, totalMicros;
    private static String lastReason = "not_run";

    public static void tick(MinecraftServer server) {
        if (owner != server) reset(server);
        long started = System.nanoTime();
        PopulationSavedData population = PopulationSavedData.get(server);
        if (population.readOnly()) return;
        var batch=population.citizenBatch(cursor,CivitasConfig.NPC_MATERIALIZATION_BATCH_SIZE.get());
        if (batch.records().isEmpty()) { PREWARMING.clear(); return; }
        for (CitizenRecord citizen:batch.records()) {
            process(server, population, citizen);
            decisions++;
        }
        cursor=batch.nextCursor();
        totalMicros += (System.nanoTime() - started) / 1_000;
    }

    private static void process(MinecraftServer server, PopulationSavedData population, CitizenRecord citizen) {
        if(!citizen.alive()){PREWARMING.remove(citizen.id());lastReason="permanently_dead";return;}
        boolean entityPresent = MaterializationLeaseManager.active(citizen.id());
        if(entityPresent){PREWARMING.remove(citizen.id());return;}
        long serverNow=server.overworld().getGameTime();
        if(citizen.runtimeState()==CitizenRuntimeState.LOCKED&&citizen.runtimeLockUntilTick()>serverNow){lastReason="locked_entity_unloaded";return;}
        Node node = node(server, citizen).orElse(null);
        double nearest = node == null ? Double.POSITIVE_INFINITY : nearestPlayer(node.level, node.pos);
        boolean locked = entityPresent && citizen.runtimeState() == CitizenRuntimeState.LOCKED;
        MaterializationRules.Decision decision = rules().evaluate(citizen.runtimeState(), nearest, node != null, entityPresent, locked);
        lastReason = decision.reason();

        if (entityPresent) {
            PREWARMING.remove(citizen.id());
            if(decision.state()!=citizen.runtimeState())transition(population,citizen,decision.state());
            return;
        }
        if (citizen.runtimeState() == CitizenRuntimeState.MATERIALIZED || citizen.runtimeState() == CitizenRuntimeState.LOCKED) {
            var recovered=transition(population,citizen,CitizenRuntimeState.VIRTUAL);if(recovered!=null)citizen=recovered;
            staleResults++;
        }
        if (decision.state() == CitizenRuntimeState.VIRTUAL) {
            PREWARMING.remove(citizen.id());
            if(citizen.runtimeState()!=CitizenRuntimeState.VIRTUAL)transition(population,citizen,CitizenRuntimeState.VIRTUAL);
            if (node == null) rejectedNode++;
            return;
        }
        long now = node.level.getGameTime();
        Long existing=PREWARMING.get(citizen.id());
        if(existing==null&&PREWARMING.size()>=CivitasConfig.NPC_PREWARM_CAP.get()){rejectedPrewarm++;lastReason="prewarm_cap";return;}
        long began=existing==null?now:existing;if(existing==null)PREWARMING.put(citizen.id(),now);
        if (citizen.runtimeState() != CitizenRuntimeState.PREWARMING) {
            var prepared=transition(population,citizen,CitizenRuntimeState.PREWARMING);if(prepared==null)return;citizen=prepared;
        }
        if (decision.state() != CitizenRuntimeState.MATERIALIZED || now - began < CivitasConfig.NPC_PREWARM_TICKS.get()) return;
        if(now-began>=CivitasConfig.NPC_PREWARM_TIMEOUT_TICKS.get()){PREWARMING.remove(citizen.id());transition(population,citizen,CitizenRuntimeState.VIRTUAL);rejectedPrewarm++;lastReason="prewarm_timeout";return;}
        if (MaterializationLeaseManager.metrics().active() >= CivitasConfig.NPC_ENTITY_CAP.get()) { rejectedCap++; lastReason = "entity_cap"; return; }
        if (!safe(node.level, node.pos)) { rejectedNode++; lastReason = "unsafe_or_unloaded_node"; return; }
        CitizenEntity entity = CivitasEntities.CITIZEN.get().create(node.level, EntitySpawnReason.EVENT);
        if (entity == null) { rejectedNode++; lastReason = "entity_create_failed"; return; }
        CitizenRecord materialized=transition(population,citizen,CitizenRuntimeState.MATERIALIZED);if(materialized==null)return;
        entity.snapTo(node.pos.getX() + .5, node.pos.getY(), node.pos.getZ() + .5, 0, 0);
        entity.bind(materialized, UUID.randomUUID(), now);
        if(entity.isRemoved()){transition(population,materialized,CitizenRuntimeState.VIRTUAL);staleResults++;return;}
        if(!node.level.noCollision(entity)||!node.level.addFreshEntity(entity)){entity.discard();transition(population,materialized,CitizenRuntimeState.VIRTUAL);staleResults++;lastReason="spawn_rejected";return;}
        PREWARMING.remove(citizen.id());
        spawned++;
    }

    public static boolean shouldVirtualize(CitizenEntity entity, ServerLevel level) {
        if (entity.locked(level.getGameTime())) return false;
        int prewarm = Math.max(CivitasConfig.NPC_MATERIALIZE_DISTANCE.get(), CivitasConfig.NPC_PREWARM_DISTANCE.get());
        int exit = Math.max(prewarm, CivitasConfig.NPC_EXIT_DISTANCE.get());
        boolean far = level.players().stream().noneMatch(player -> player.distanceToSqr(entity) <= (double) exit * exit);
        if (far) virtualized++;
        return far;
    }

    /** Immediately removes the visible representation after the authority commits permanent death. */
    public static void discardPermanentDeath(MinecraftServer server,UUID citizenId){var lease=MaterializationLeaseManager.lease(citizenId).orElse(null);if(lease!=null)for(ServerLevel level:server.getAllLevels()){var entity=level.getEntity(lease.entityId());if(entity instanceof CitizenEntity citizen){CitizenEquipmentDeathDropManager.dropVisible(citizen);citizen.discard();break;}if(entity!=null){entity.discard();break;}}MaterializationLeaseManager.revoke(citizenId);PREWARMING.remove(citizenId);}

    private static Optional<Node> node(MinecraftServer server, CitizenRecord citizen) {
        PopulationSavedData population=PopulationSavedData.get(server);
        var location=population.location(citizen.id()).orElse(null);
        if(location!=null&&citizen.cityId()!=null&&citizen.cityId().equals(location.cityId())){
            var city=CitySavedData.get(server).byId(citizen.cityId()).orElse(null);
            if(city!=null&&city.dimension().equals(location.dimension()))for(ServerLevel level:server.getAllLevels())
                if(level.dimension().identifier().toString().equals(location.dimension()))return Optional.of(new Node(level,BlockPos.of(location.position())));
        }
        BuildingSavedData buildings = BuildingSavedData.get(server);
        BuildingRecord building=valid(buildings,citizen.residenceId(),citizen.cityId()).orElse(null);
        if(building==null)building=valid(buildings,citizen.workBuildingId(),citizen.cityId()).orElse(null);
        if(building==null)return Optional.empty();
        for (ServerLevel level : server.getAllLevels())
            if (level.dimension().identifier().toString().equals(building.dimension())) return Optional.of(new Node(level, BlockPos.of(building.interior())));
        return Optional.empty();
    }

    private static boolean safe(ServerLevel level, BlockPos pos) {
        return level.isInWorldBounds(pos) && level.hasChunkAt(pos)
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && level.getBlockState(pos.below()).blocksMotion()
                && level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty();
    }

    private static Optional<BuildingRecord> valid(BuildingSavedData buildings,UUID id,UUID cityId){return id==null?Optional.empty():buildings.byId(id).filter(b->b.status()==BuildingStatus.VALID&&b.cityId().equals(cityId));}
    private static CitizenRecord transition(PopulationSavedData data,CitizenRecord citizen,CitizenRuntimeState state){var result=TRANSITIONS.transition(data,citizen.id(),citizen.revision(),state);if(!result.success()){staleResults++;lastReason=result.failure().name().toLowerCase(java.util.Locale.ROOT);return null;}return result.citizen();}

    private static double nearestPlayer(ServerLevel level, BlockPos pos) {
        double nearest = Double.POSITIVE_INFINITY;
        for (ServerPlayer player : level.players()) nearest = Math.min(nearest, Math.sqrt(player.distanceToSqr(pos.getX() + .5, pos.getY(), pos.getZ() + .5)));
        return nearest;
    }

    private static MaterializationRules rules() {
        int materialize = CivitasConfig.NPC_MATERIALIZE_DISTANCE.get();
        int prewarm = Math.max(materialize, CivitasConfig.NPC_PREWARM_DISTANCE.get());
        return new MaterializationRules(materialize, prewarm, Math.max(prewarm, CivitasConfig.NPC_EXIT_DISTANCE.get()));
    }

    private static void reset(MinecraftServer server) {
        owner = server; cursor = null; PREWARMING.clear(); MaterializationLeaseManager.clear();
        decisions = spawned = virtualized = rejectedCap = rejectedPrewarm = rejectedNode = staleResults = totalMicros = 0; lastReason = "server_reset";
    }

    public static Metrics metrics() { return new Metrics(PREWARMING.size(), decisions, spawned, virtualized,
            rejectedCap,rejectedPrewarm,rejectedNode,staleResults,decisions == 0 ? 0 : totalMicros / decisions,lastReason); }
    public record Metrics(int prewarming, long decisions, long spawned, long virtualized, long rejectedCap,long rejectedPrewarm,
            long rejectedNode, long staleResults, long averageDecisionMicros, String lastReason) {}
    private record Node(ServerLevel level, BlockPos pos) {}
    private CitizenMaterializationManager() {}
}
