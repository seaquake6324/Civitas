package com.seaquake6324.civitas.infrastructure.world;

import com.seaquake6324.civitas.infrastructure.registry.CivitasBlocks;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

public final class CityCoreBlockEntity extends BlockEntity {
    @Nullable private UUID cityId;
    private boolean activated;
    @Nullable private UUID placerId;
    private String cityName = "";
    private int cityColor;
    private long activatedAt;

    public CityCoreBlockEntity(BlockPos pos, BlockState state) { super(CivitasBlocks.CITY_CORE_ENTITY.get(), pos, state); }
    public boolean isActivated() { return activated; }
    @Nullable public UUID cityId() { return cityId; }
    @Nullable public UUID placerId() { return placerId; }
    public String cityName() { return cityName; }
    public int cityColor() { return cityColor; }
    public long activatedAt() { return activatedAt; }

    public void setPlacer(UUID placerId) { this.placerId = placerId; setChanged(); }
    public void activate(UUID cityId, String name, int color, long time) {
        this.cityId = cityId;
        this.cityName = name;
        this.cityColor = color & 0xFFFFFF;
        this.activatedAt = time;
        this.activated = true;
        setChanged();
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (cityId != null) output.putString("CityId", cityId.toString());
        output.putBoolean("Activated", activated);
        if (placerId != null) output.putString("PlacerId", placerId.toString());
        output.putString("CityName", cityName);
        output.putInt("CityColor", cityColor);
        output.putLong("ActivatedAt", activatedAt);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        cityId = input.getString("CityId").flatMap(CityCoreBlockEntity::readUuid).orElse(null);
        activated = input.getBooleanOr("Activated", false);
        placerId = input.getString("PlacerId").flatMap(CityCoreBlockEntity::readUuid).orElse(null);
        cityName = input.getStringOr("CityName", "");
        cityColor = input.getIntOr("CityColor", 0);
        activatedAt = input.getLongOr("ActivatedAt", 0L);
    }

    private static java.util.Optional<UUID> readUuid(String value) {
        try {
            return java.util.Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
