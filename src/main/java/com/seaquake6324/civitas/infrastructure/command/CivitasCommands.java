package com.seaquake6324.civitas.infrastructure.command;

import com.seaquake6324.civitas.infrastructure.network.CityMapNetworkSync;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.persistence.CivilizationSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.BuildingSavedData;
import com.seaquake6324.civitas.infrastructure.world.CityCoreBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.seaquake6324.civitas.infrastructure.debug.RegionDebugManager;
import com.seaquake6324.civitas.infrastructure.debug.AdminDebugTools;
import com.seaquake6324.civitas.infrastructure.border.BorderThreatManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.seaquake6324.civitas.domain.population.Gender;

public final class CivitasCommands {
    public static void register() { NeoForge.EVENT_BUS.addListener(CivitasCommands::onRegisterCommands); }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("cvt")
                .requires(source -> source.getPlayer() != null && source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("remove").executes(context -> removeLookedAtCore(context.getSource())))
                .then(Commands.literal("gender")
                        .then(Commands.literal("male").then(Commands.argument("target",StringArgumentType.greedyString()).executes(c->GenderCommandHandler.change(c.getSource(),Gender.MALE,StringArgumentType.getString(c,"target")))))
                        .then(Commands.literal("female").then(Commands.argument("target",StringArgumentType.greedyString()).executes(c->GenderCommandHandler.change(c.getSource(),Gender.FEMALE,StringArgumentType.getString(c,"target"))))))
                .then(Commands.literal("debug")
                        .then(Commands.literal("on").executes(context -> setRegionDebug(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setRegionDebug(context.getSource(), false)))
                        .then(Commands.literal("rescan").executes(c->AdminDebugTools.rescan(c.getSource().getPlayer())))
                        .then(Commands.literal("candidate").executes(c->AdminDebugTools.submitCandidate(c.getSource().getPlayer())))
                        .then(Commands.literal("evolve").executes(c->AdminDebugTools.evolve(c.getSource().getPlayer())))
                        .then(Commands.literal("settle-activity").executes(c->AdminDebugTools.settleActivity(c.getSource().getPlayer())))
                        .then(Commands.literal("activity").then(Commands.argument("value",IntegerArgumentType.integer(0,100)).executes(c->AdminDebugTools.setActivity(c.getSource().getPlayer(),IntegerArgumentType.getInteger(c,"value")))))
                        .then(Commands.literal("expansion").executes(c->AdminDebugTools.expansionBreakdown(c.getSource().getPlayer())))
                        .then(Commands.literal("pressure-reset").executes(c->AdminDebugTools.pressure(c.getSource().getPlayer(),0,false,false)))
                        .then(Commands.literal("pressure").then(Commands.argument("value",IntegerArgumentType.integer(0,100)).executes(c->AdminDebugTools.pressure(c.getSource().getPlayer(),IntegerArgumentType.getInteger(c,"value"),false,false))))
                        .then(Commands.literal("pressure-add").then(Commands.argument("value",IntegerArgumentType.integer(1,100)).executes(c->AdminDebugTools.pressure(c.getSource().getPlayer(),IntegerArgumentType.getInteger(c,"value"),true,false))))
                        .then(Commands.literal("invasion").executes(c->AdminDebugTools.pressure(c.getSource().getPlayer(),100,false,true)))
                        .then(Commands.literal("threat").executes(c->AdminDebugTools.threatPerformance(c.getSource().getPlayer())))
                        .then(Commands.literal("battle").executes(c->AdminDebugTools.battle(c.getSource().getPlayer())))
                        .then(Commands.literal("neglect").executes(c->AdminDebugTools.neglect(c.getSource().getPlayer(),false)))
                        .then(Commands.literal("neglect-reset").executes(c->AdminDebugTools.neglect(c.getSource().getPlayer(),true)))
                        .then(Commands.literal("topology").executes(c->AdminDebugTools.topology(c.getSource().getPlayer())))
                        .then(Commands.literal("building").executes(c->AdminDebugTools.building(c.getSource().getPlayer())))
                        .then(Commands.literal("security").executes(c->AdminDebugTools.security(c.getSource().getPlayer())))
                        .then(Commands.literal("patrol").executes(c->AdminDebugTools.patrol(c.getSource().getPlayer())))
                        .then(Commands.literal("population").executes(c->AdminDebugTools.population(c.getSource().getPlayer())))
                        .then(Commands.literal("reproduction").executes(c->AdminDebugTools.reproduction(c.getSource().getPlayer())))
                        .then(Commands.literal("migration").executes(c->AdminDebugTools.migration(c.getSource().getPlayer())))
                        .then(Commands.literal("population-migration").executes(c->AdminDebugTools.outMigration(c.getSource().getPlayer())))
                        .then(Commands.literal("citizen").then(Commands.argument("race",StringArgumentType.word())
                                .then(Commands.argument("gender",StringArgumentType.word()).executes(c->AdminDebugTools.createCitizen(c.getSource().getPlayer(),StringArgumentType.getString(c,"race"),StringArgumentType.getString(c,"gender"))))))));
    }

    private static int setRegionDebug(CommandSourceStack source, boolean enabled) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        if (enabled) RegionDebugManager.enable(player); else RegionDebugManager.disable(player);
        return 1;
    }

    private static int removeLookedAtCore(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        HitResult hit = player.pick(8.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || !(player.level().getBlockEntity(blockHit.getBlockPos()) instanceof CityCoreBlockEntity core)) {
            player.sendSystemMessage(Component.translatable("civitas.command.remove.no_core"), true);
            return 0;
        }
        if (core.cityId() != null) CitySavedData.get(source.getServer()).remove(core.cityId()).ifPresent(city -> {
            BuildingSavedData.get(source.getServer()).removeCity(city.id());
            BorderThreatManager.removeCity(source.getServer(),city.id());
            CivilizationSavedData civilization = CivilizationSavedData.get(source.getServer());
            city.territory().forEach(chunk -> civilization.removeChunk(city.dimension(), chunk));
            CityMapNetworkSync.broadcastRemove(core.cityId());
        });
        player.level().removeBlock(blockHit.getBlockPos(), false);
        player.sendSystemMessage(Component.translatable("civitas.command.remove.success"), true);
        return 1;
    }

    private CivitasCommands() {}
}
