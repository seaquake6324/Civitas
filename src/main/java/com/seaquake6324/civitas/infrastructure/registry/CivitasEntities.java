package com.seaquake6324.civitas.infrastructure.registry;

import com.seaquake6324.civitas.CivitasMod;
import com.seaquake6324.civitas.infrastructure.entity.CitizenEntity;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.*;

public final class CivitasEntities{
 private static final DeferredRegister.Entities ENTITIES=DeferredRegister.createEntities(CivitasMod.MOD_ID);
 public static final DeferredHolder<EntityType<?>,EntityType<CitizenEntity>>CITIZEN=ENTITIES.registerEntityType("citizen",CitizenEntity::new,MobCategory.CREATURE,b->b.sized(.6F,1.8F).clientTrackingRange(10).updateInterval(2));
 public static void register(IEventBus bus){ENTITIES.register(bus);bus.addListener(CivitasEntities::attributes);}private static void attributes(EntityAttributeCreationEvent event){event.put(CITIZEN.get(),CitizenEntity.createAttributes().build());}
 private CivitasEntities(){}
}
