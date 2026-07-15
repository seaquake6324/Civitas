package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SubmitCityManagementPayload(BlockPos corePos, String cityName, int cityColor) implements CustomPacketPayload {
    public static final Type<SubmitCityManagementPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "submit_city_management"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitCityManagementPayload> STREAM_CODEC = CustomPacketPayload.codec(
            SubmitCityManagementPayload::write, buffer -> new SubmitCityManagementPayload(buffer.readBlockPos(), buffer.readUtf(80), buffer.readInt()));
    private void write(RegistryFriendlyByteBuf buffer) { buffer.writeBlockPos(corePos); buffer.writeUtf(cityName, 80); buffer.writeInt(cityColor); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
