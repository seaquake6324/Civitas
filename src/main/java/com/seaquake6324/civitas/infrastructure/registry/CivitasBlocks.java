package com.seaquake6324.civitas.infrastructure.registry;

import com.seaquake6324.civitas.CivitasMod;
import com.seaquake6324.civitas.infrastructure.world.CityCoreBlock;
import com.seaquake6324.civitas.infrastructure.world.CityCoreBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CivitasBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CivitasMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CivitasMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CivitasMod.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CivitasMod.MOD_ID);

    public static final DeferredBlock<CityCoreBlock> CITY_CORE = BLOCKS.registerBlock("city_core", CityCoreBlock::new,
            () -> BlockBehaviour.Properties.of().strength(3.5F, 3_600_000F).sound(SoundType.STONE)
                    .lightLevel(state -> 5).pushReaction(PushReaction.BLOCK).noLootTable());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CityCoreBlockEntity>> CITY_CORE_ENTITY =
            BLOCK_ENTITIES.register("city_core", () -> new BlockEntityType<>(CityCoreBlockEntity::new, CITY_CORE.get()));
    public static final DeferredItem<BlockItem> CITY_CORE_ITEM = ITEMS.registerSimpleBlockItem(CITY_CORE);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CITY_TAB = CREATIVE_TABS.register("city", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.civitas.city"))
                    .icon(() -> new ItemStack(CITY_CORE_ITEM.get()))
                    .displayItems((parameters, output) -> output.accept(CITY_CORE_ITEM.get()))
                    .build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        CREATIVE_TABS.register(bus);
    }

    private CivitasBlocks() {}
}
