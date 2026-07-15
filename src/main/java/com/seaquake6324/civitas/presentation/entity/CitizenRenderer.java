package com.seaquake6324.civitas.presentation.entity;

import com.seaquake6324.civitas.CivitasMod;
import com.seaquake6324.civitas.domain.population.CitizenRace;
import com.seaquake6324.civitas.infrastructure.entity.CitizenEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.Identifier;

/** Shared humanoid rig; race and appearance differ only through texture in the first slice. */
public final class CitizenRenderer extends HumanoidMobRenderer<CitizenEntity,CitizenRenderState,HumanoidModel<CitizenRenderState>>{
 public CitizenRenderer(EntityRendererProvider.Context context){super(context,new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_BABY)),.5F);}
 @Override public CitizenRenderState createRenderState(){return new CitizenRenderState();}
 @Override public void extractRenderState(CitizenEntity entity,CitizenRenderState state,float partialTick){super.extractRenderState(entity,state,partialTick);state.race=entity.citizenRace();state.appearance=entity.appearanceIndex();}
 @Override public Identifier getTextureLocation(CitizenRenderState state){String name=switch(state.race){case HUMAN->"human_"+Math.floorMod(state.appearance,4);case PIGFOLK->"pigfolk_0";case COWFOLK->"cowfolk_0";case SHEEPFOLK->"sheepfolk_0";};return Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID,"textures/entity/citizen/"+name+".png");}
}
