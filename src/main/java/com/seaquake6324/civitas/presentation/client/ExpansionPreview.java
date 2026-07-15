package com.seaquake6324.civitas.presentation.client;

import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.territory.TerritoryTopology;
import com.seaquake6324.civitas.infrastructure.network.ExpansionModePayload;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;

/** Lightweight client hint; the server remains authoritative for every expansion condition. */
public final class ExpansionPreview {
    private static UUID cityId;
    private static Set<Long> candidates = Set.of(),eligible=Set.of();
    private static ChunkPos target;
    private static boolean adjacent;

    public static void accept(ExpansionModePayload payload) {
        if (!payload.active()) { clear(); return; }
        cityId = payload.cityId(); candidates=payload.candidates();eligible=payload.eligible();
    }
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (cityId == null || minecraft.player == null) return;
        target = ChunkPos.containing(minecraft.player.blockPosition());
        long packed = ChunkCoordinate.pack(target.x(), target.z());
        adjacent = eligible.contains(packed);
    }
    public static ChunkPos target() { return target; }
    public static boolean adjacent() { return adjacent; }
    public static boolean candidate(){return target!=null&&candidates.contains(ChunkCoordinate.pack(target.x(),target.z()));}
    public static Set<Long> candidates(){return candidates;}
    public static boolean eligible(long chunk){return eligible.contains(chunk);}
    public static void clear() { cityId=null;candidates=Set.of();eligible=Set.of();target=null;adjacent=false; }
    private ExpansionPreview() {}
}
