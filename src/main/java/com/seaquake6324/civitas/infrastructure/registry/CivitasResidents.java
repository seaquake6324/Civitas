package com.seaquake6324.civitas.infrastructure.registry;

import com.seaquake6324.civitas.CivitasMod;
import com.seaquake6324.civitas.domain.population.AgeStage;
import com.seaquake6324.civitas.domain.population.CitizenRace;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Creative-only resident spawn eggs; each egg materializes one persistent domain citizen. */
public final class CivitasResidents {
    private static final int CREATIVE_TAB_COLUMNS = 9;
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CivitasMod.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CivitasMod.MOD_ID);
    private static final Map<CitizenRace, Map<AgeStage, DeferredItem<SpawnEggItem>>> EGGS = registerEggs();
    private static final DeferredItem<Item> TAB_SPACER = ITEMS.registerSimpleItem("resident_tab_spacer", Item.Properties::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RESIDENT_TAB = TABS.register("residents", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.civitas.residents"))
            .icon(() -> new ItemStack(egg(CitizenRace.HUMAN, AgeStage.YOUNG_ADULT).get()))
            .displayItems((parameters, output) -> addResidentRows(output))
            .build());

    public static DeferredItem<SpawnEggItem> egg(CitizenRace race, AgeStage age) {
        return EGGS.get(race).get(age);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private static void addResidentRows(CreativeModeTab.Output output) {
        int row = 0;
        for (CitizenRace race : CitizenRace.values()) {
            for (AgeStage age : AgeStage.values()) {
                output.accept(egg(race, age).get());
            }
            int spacerCount = CREATIVE_TAB_COLUMNS - AgeStage.values().length;
            for (int column = 0; column < spacerCount; column++) {
                ItemStack spacer = new ItemStack(TAB_SPACER.get());
                CompoundTag layoutKey = new CompoundTag();
                layoutKey.putInt("Row", row);
                layoutKey.putInt("Column", column);
                spacer.set(DataComponents.CUSTOM_DATA, CustomData.of(layoutKey));
                output.accept(spacer, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
            }
            row++;
        }
    }

    private static Map<CitizenRace, Map<AgeStage, DeferredItem<SpawnEggItem>>> registerEggs() {
        EnumMap<CitizenRace, Map<AgeStage, DeferredItem<SpawnEggItem>>> all = new EnumMap<>(CitizenRace.class);
        for (CitizenRace race : CitizenRace.values()) {
            EnumMap<AgeStage, DeferredItem<SpawnEggItem>> ages = new EnumMap<>(AgeStage.class);
            for (AgeStage age : AgeStage.values()) {
                String id = race.name().toLowerCase(java.util.Locale.ROOT) + "_" + age.name().toLowerCase(java.util.Locale.ROOT) + "_spawn_egg";
                ages.put(age, ITEMS.registerItem(id, properties -> new SpawnEggItem(spawnEgg(properties, race, age)), Item.Properties::new));
            }
            all.put(race, Map.copyOf(ages));
        }
        return Map.copyOf(all);
    }

    private static Item.Properties spawnEgg(Item.Properties properties, CitizenRace race, AgeStage age) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("CivitasSpawnEgg", true);
        tag.putString("CivitasRace", race.name());
        tag.putString("CivitasAgeStage", age.name());
        return properties.component(DataComponents.ENTITY_DATA, TypedEntityData.of(CivitasEntities.CITIZEN.get(), tag));
    }

    private CivitasResidents() {}
}
