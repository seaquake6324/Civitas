package com.seaquake6324.civitas;

import com.seaquake6324.civitas.infrastructure.command.CivitasCommands;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.config.CivitasClientConfig;
import com.seaquake6324.civitas.infrastructure.event.CivitasGameEvents;
import com.seaquake6324.civitas.infrastructure.network.CivitasNetwork;
import com.seaquake6324.civitas.infrastructure.registry.CivitasBlocks;
import com.seaquake6324.civitas.infrastructure.registry.CivitasEntities;
import com.seaquake6324.civitas.infrastructure.registry.CivitasResidents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(CivitasMod.MOD_ID)
public final class CivitasMod {
    public static final String MOD_ID = "civitas";

    public CivitasMod(IEventBus modEventBus, ModContainer modContainer) {
        CivitasEntities.register(modEventBus);
        CivitasBlocks.register(modEventBus);
        CivitasResidents.register(modEventBus);
        modEventBus.addListener(CivitasNetwork::register);
        modContainer.registerConfig(ModConfig.Type.SERVER, CivitasConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CivitasClientConfig.SPEC);
        CivitasGameEvents.register();
        CivitasCommands.register();
    }
}
