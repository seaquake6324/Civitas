package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Bounded client request for a fresh server-derived resident card. */
public record RequestCitizenInfoPayload(UUID citizenId) implements CustomPacketPayload {
    public static final Type<RequestCitizenInfoPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID, "request_citizen_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestCitizenInfoPayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    (payload, buffer) -> buffer.writeUUID(payload.citizenId),
                    buffer -> new RequestCitizenInfoPayload(buffer.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
