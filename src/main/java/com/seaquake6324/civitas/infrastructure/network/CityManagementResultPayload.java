package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CityManagementResultPayload(boolean success, String messageKey) implements CustomPacketPayload {
    public static final Type<CityManagementResultPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "city_management_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CityManagementResultPayload> STREAM_CODEC = CustomPacketPayload.codec(
            CityManagementResultPayload::write, buffer -> new CityManagementResultPayload(buffer.readBoolean(), buffer.readUtf(128)));
    private void write(RegistryFriendlyByteBuf buffer) { buffer.writeBoolean(success); buffer.writeUtf(messageKey, 128); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
