package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TerritoryDebugPayload(
        boolean owned, String city, int territory, int heartland, boolean connected, int externalEdges,
        int citySchema, String cityMigration, boolean cityReadOnly,
        String stage, long claimedAt, long sourceChunk, long neglectStartedAt, long recoveryRemaining,
        float civility, float targetCivility, float activity,
        float readiness, float readinessCivility, float readinessActivity, float readinessSafety,
        float readinessFortification, int scannedColumns, int wallColumns, int entrances, int gaps,
        int gates, int insidePaths, float pressure, float pressureReadinessGain, float pressureSizeGain,
        float pressureBufferReduction, float pressureFinalGain, long threatCooldownRemaining,
        String threatPhase, String direction, int wave, int failedDefenses,
        int spawnX, int spawnY, int spawnZ) implements CustomPacketPayload {
    public static final Type<TerritoryDebugPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "territory_debug"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerritoryDebugPayload> STREAM_CODEC =
            CustomPacketPayload.codec(TerritoryDebugPayload::write, TerritoryDebugPayload::read);

    public static TerritoryDebugPayload empty() {
        return new TerritoryDebugPayload(false, "", 0, 0, true, 0, 0, "", false,
                "", 0L, Long.MIN_VALUE, 0L, 0L,
                0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F,
                0, 0, 0, 0, 0, 0,
                0F, 0F, 0F, 0F, 0F, 0L,
                "", "", 0, 0, 0, 0, 0);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(owned); buffer.writeUtf(city); buffer.writeVarInt(territory);
        buffer.writeVarInt(heartland); buffer.writeBoolean(connected); buffer.writeVarInt(externalEdges);
        buffer.writeVarInt(citySchema); buffer.writeUtf(cityMigration); buffer.writeBoolean(cityReadOnly);
        buffer.writeUtf(stage); buffer.writeVarLong(claimedAt); buffer.writeLong(sourceChunk);
        buffer.writeVarLong(neglectStartedAt); buffer.writeVarLong(recoveryRemaining);
        buffer.writeFloat(civility); buffer.writeFloat(targetCivility); buffer.writeFloat(activity);
        buffer.writeFloat(readiness); buffer.writeFloat(readinessCivility); buffer.writeFloat(readinessActivity);
        buffer.writeFloat(readinessSafety); buffer.writeFloat(readinessFortification);
        buffer.writeVarInt(scannedColumns); buffer.writeVarInt(wallColumns); buffer.writeVarInt(entrances);
        buffer.writeVarInt(gaps); buffer.writeVarInt(gates); buffer.writeVarInt(insidePaths);
        buffer.writeFloat(pressure); buffer.writeFloat(pressureReadinessGain); buffer.writeFloat(pressureSizeGain);
        buffer.writeFloat(pressureBufferReduction); buffer.writeFloat(pressureFinalGain);
        buffer.writeVarLong(threatCooldownRemaining); buffer.writeUtf(threatPhase); buffer.writeUtf(direction);
        buffer.writeVarInt(wave); buffer.writeVarInt(failedDefenses);
        buffer.writeInt(spawnX); buffer.writeInt(spawnY); buffer.writeInt(spawnZ);
    }

    private static TerritoryDebugPayload read(RegistryFriendlyByteBuf buffer) {
        return new TerritoryDebugPayload(buffer.readBoolean(), buffer.readUtf(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readUtf(), buffer.readBoolean(), buffer.readUtf(), buffer.readVarLong(),
                buffer.readLong(), buffer.readVarLong(), buffer.readVarLong(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readVarLong(), buffer.readUtf(), buffer.readUtf(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readInt(), buffer.readInt(), buffer.readInt());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
