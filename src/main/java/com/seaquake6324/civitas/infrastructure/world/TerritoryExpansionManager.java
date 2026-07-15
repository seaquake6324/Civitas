package com.seaquake6324.civitas.infrastructure.world;

import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.civilization.CivilizationLayer;
import com.seaquake6324.civitas.domain.territory.ExpansionEligibility;
import com.seaquake6324.civitas.domain.territory.TerritoryTopology;
import com.seaquake6324.civitas.infrastructure.civilization.CivilityDirtyQueue;
import com.seaquake6324.civitas.infrastructure.civilization.CivilityScanScheduler;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.network.CityMapNetworkSync;
import com.seaquake6324.civitas.infrastructure.network.ExpansionModePayload;
import com.seaquake6324.civitas.infrastructure.network.ExpansionResultPayload;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.persistence.CivilizationSavedData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-authoritative expansion mode; client eligibility colors are only previews. */
public final class TerritoryExpansionManager {
    private static final Map<UUID, UUID> ACTIVE = new HashMap<>();

    public static boolean active(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }

    public static void begin(ServerPlayer player, City city) {
        if (!city.mayManage(player.getUUID())) return;
        ACTIVE.put(player.getUUID(), city.id());
        Preview preview=preview((ServerLevel)player.level(),city);
        PacketDistributor.sendToPlayer(player, new ExpansionModePayload(true, city.id(), preview.candidates(),preview.eligible()));
        player.sendSystemMessage(Component.translatable("civitas.expansion.started"), true);
    }

    public static void remove(ServerPlayer player) {
        if (ACTIVE.remove(player.getUUID()) != null) PacketDistributor.sendToPlayer(player, ExpansionModePayload.stopped());
    }

    public static boolean cancelAtCore(ServerPlayer player, com.seaquake6324.civitas.infrastructure.world.CityCoreBlockEntity core) {
        UUID activeCity = ACTIVE.get(player.getUUID());
        if (activeCity == null || core.cityId() == null || !activeCity.equals(core.cityId())) return false;
        remove(player);
        player.sendSystemMessage(Component.translatable("civitas.expansion.cancelled"), true);
        return true;
    }

    public static void confirm(ServerLevel level, ServerPlayer player, long target) {
        UUID cityId = ACTIVE.get(player.getUUID());
        if (cityId == null) return;
        CitySavedData repository = CitySavedData.get(level.getServer());
        City city = repository.byId(cityId).orElse(null);
        if (city == null) { remove(player); return; }

        Map<Long, ExpansionEligibility.ChunkHealth> health = health(level, city);
        var rules = new ExpansionEligibility.Rules(CivitasConfig.EXPANSION_CIVILITY_MINIMUM.get(),
                CivitasConfig.EXPANSION_ACTIVITY_MINIMUM.get(), CivitasConfig.EXPANSION_DEVELOPED_COVERAGE.get(),
                CivitasConfig.EXPANSION_ACTIVE_COVERAGE.get(), CivitasConfig.EXPANSION_COOLDOWN_TICKS.get());
        long source = source(city,target,health,rules);
        boolean claimed = repository.cityAt(city.dimension(), target).isPresent();
        boolean bufferConflict = repository.cities().stream()
                .filter(other -> !other.id().equals(city.id()) && other.dimension().equals(city.dimension()))
                .flatMap(other -> other.territory().stream())
                .anyMatch(chunk -> ChunkCoordinate.unpack(chunk).chebyshevDistance(ChunkCoordinate.unpack(target))
                        <= CivitasConfig.BORDER_BUFFER_CHUNKS.get());
        ChunkCoordinate coordinate = ChunkCoordinate.unpack(target);
        var context = new ExpansionEligibility.Context(city.mayManage(player.getUUID()),
                CivitasConfig.ALLOWED_DIMENSIONS.get().contains(city.dimension()), level.hasChunk(coordinate.x(), coordinate.z()),
                claimed, bufferConflict, level.getGameTime(), target, source, health);
        ExpansionEligibility.Result result = ExpansionEligibility.evaluate(city, context, rules);
        if (!result.eligible()) {
            PacketDistributor.sendToPlayer(player, new ExpansionResultPayload(false,
                    "civitas.expansion.failure." + result.failures().getFirst().name().toLowerCase(java.util.Locale.ROOT)));
            return;
        }

        City updated = city.claim(target, source, level.getGameTime());
        repository.add(updated);
        CityMapNetworkSync.broadcastUpsert(level.getServer(), updated);
        markBoundaryDirty(level,target);
        expansionEffect(level, coordinate, city.color());
        remove(player);
        PacketDistributor.sendToPlayer(player, new ExpansionResultPayload(true, "civitas.expansion.success"));
    }

    private static Map<Long, ExpansionEligibility.ChunkHealth> health(ServerLevel level, City city) {
        Map<Long, ExpansionEligibility.ChunkHealth> result = new HashMap<>();
        CivilizationSavedData data = CivilizationSavedData.get(level.getServer());
        for (long chunk : city.territory()) {
            var surface = data.get(city.dimension(), chunk, CivilizationLayer.SURFACE);
            var underground = data.get(city.dimension(), chunk, CivilizationLayer.UNDERGROUND);
            result.put(chunk, new ExpansionEligibility.ChunkHealth(surface.civility(), surface.activity(),
                    underground.civility(), underground.activity()));
        }
        return result;
    }

    private static Preview preview(ServerLevel level,City city){
        Set<Long> candidates=new HashSet<>();for(long chunk:city.territory())for(var direction:TerritoryTopology.Direction.values()){long target=TerritoryTopology.adjacent(chunk,direction);if(!city.territory().contains(target))candidates.add(target);}Set<Long> eligible=new HashSet<>();Map<Long,ExpansionEligibility.ChunkHealth> health=health(level,city);CitySavedData repo=CitySavedData.get(level.getServer());var rules=new ExpansionEligibility.Rules(CivitasConfig.EXPANSION_CIVILITY_MINIMUM.get(),CivitasConfig.EXPANSION_ACTIVITY_MINIMUM.get(),CivitasConfig.EXPANSION_DEVELOPED_COVERAGE.get(),CivitasConfig.EXPANSION_ACTIVE_COVERAGE.get(),CivitasConfig.EXPANSION_COOLDOWN_TICKS.get());for(long target:candidates){long source=source(city,target,health,rules);boolean claimed=repo.cityAt(city.dimension(),target).isPresent();boolean buffer=repo.cities().stream().filter(other->!other.id().equals(city.id())&&other.dimension().equals(city.dimension())).flatMap(other->other.territory().stream()).anyMatch(c->ChunkCoordinate.unpack(c).chebyshevDistance(ChunkCoordinate.unpack(target))<=CivitasConfig.BORDER_BUFFER_CHUNKS.get());ChunkCoordinate at=ChunkCoordinate.unpack(target);var context=new ExpansionEligibility.Context(true,CivitasConfig.ALLOWED_DIMENSIONS.get().contains(city.dimension()),level.hasChunk(at.x(),at.z()),claimed,buffer,level.getGameTime(),target,source,health);if(ExpansionEligibility.evaluate(city,context,rules).eligible())eligible.add(target);}return new Preview(candidates,eligible);
    }
    private record Preview(Set<Long> candidates,Set<Long> eligible){}

    private static boolean cardinallyAdjacent(long from, long to) {
        for (TerritoryTopology.Direction direction : TerritoryTopology.Direction.values())
            if (TerritoryTopology.adjacent(from, direction) == to) return true;
        return false;
    }

    private static long source(City city,long target,Map<Long,ExpansionEligibility.ChunkHealth> health,
            ExpansionEligibility.Rules rules){
        return city.territory().stream().filter(chunk->cardinallyAdjacent(chunk,target))
                .sorted((a,b)->Boolean.compare(health.getOrDefault(b,new ExpansionEligibility.ChunkHealth(0,0,0,0)).developed(rules.civilityMinimum(),rules.activityMinimum()),health.getOrDefault(a,new ExpansionEligibility.ChunkHealth(0,0,0,0)).developed(rules.civilityMinimum(),rules.activityMinimum())))
                .findFirst().orElse(Long.MIN_VALUE);
    }

    private static void markBoundaryDirty(ServerLevel level,long chunk){
        ChunkCoordinate at=ChunkCoordinate.unpack(chunk);
        CivilityScanScheduler.markDirty(level,at.x(),at.z(),CivilityDirtyQueue.Reason.BORDER_CHANGE);
        for(var direction:TerritoryTopology.Direction.values()){
            ChunkCoordinate neighbor=ChunkCoordinate.unpack(TerritoryTopology.adjacent(chunk,direction));
            CivilityScanScheduler.markDirty(level,neighbor.x(),neighbor.z(),CivilityDirtyQueue.Reason.BORDER_CHANGE);
        }
    }

    private static void expansionEffect(ServerLevel level, ChunkCoordinate chunk, int color) {
        DustParticleOptions dust = new DustParticleOptions(color, 1.1F);
        int minX = chunk.x() << 4, minZ = chunk.z() << 4;
        for (int i = 0; i <= 16; i += 2) {
            double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    minX + Math.min(i, 15), minZ + Math.min(i, 15)) + 0.25;
            level.sendParticles(dust, minX + i, y, minZ, 1, 0, .1, 0, 0);
            level.sendParticles(dust, minX + i, y, minZ + 16, 1, 0, .1, 0, 0);
            level.sendParticles(dust, minX, y, minZ + i, 1, 0, .1, 0, 0);
            level.sendParticles(dust, minX + 16, y, minZ + i, 1, 0, .1, 0, 0);
        }
    }
    private TerritoryExpansionManager() {}
}
