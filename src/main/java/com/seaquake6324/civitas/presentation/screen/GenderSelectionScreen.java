package com.seaquake6324.civitas.presentation.screen;

import com.seaquake6324.civitas.domain.population.Gender;
import com.seaquake6324.civitas.infrastructure.network.SubmitGenderSelectionPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Required first-entry profile choice. It never writes to chat. */
public final class GenderSelectionScreen extends Screen{
    private static final int WIDTH=300,HEIGHT=150;private int left,top,noticeTicks,closeTicks;private boolean waiting,allowClose;private Component notice=Component.empty();private int noticeColor=0xFFFF7777;
    public GenderSelectionScreen(){super(Component.translatable("civitas.gender.title"));}
    @Override protected void init(){left=(width-WIDTH)/2;top=(height-HEIGHT)/2;addRenderableWidget(Button.builder(Component.translatable("civitas.gender.male"),b->submit(Gender.MALE)).bounds(left+35,top+78,105,24).build());addRenderableWidget(Button.builder(Component.translatable("civitas.gender.female"),b->submit(Gender.FEMALE)).bounds(left+160,top+78,105,24).build());}
    private void submit(Gender gender){if(waiting)return;waiting=true;notice=Component.translatable("civitas.gender.saving");noticeColor=0xFFD8C7A1;noticeTicks=100;ClientPacketDistributor.sendToServer(new SubmitGenderSelectionPayload(gender));}
    public void handleResult(boolean success,String key){waiting=false;notice=Component.translatable(key);noticeColor=success?0xFF9ED6B0:0xFFFF7777;noticeTicks=100;if(success||"civitas.gender.already_selected".equals(key)){allowClose=true;closeTicks=24;}else if("civitas.gender.read_only".equals(key)){allowClose=true;closeTicks=60;}}
    @Override public void tick(){super.tick();if(noticeTicks>0)noticeTicks--;if(closeTicks>0&&--closeTicks==0)minecraft.popGuiLayer();}
    @Override public boolean shouldCloseOnEsc(){return allowClose;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick){CivitasGuiTextures.stoneCityPanel(g,left,top,WIDTH,HEIGHT);int middle=left+WIDTH/2,halfHeight=top+HEIGHT/2;g.fill(left,top,middle,top+HEIGHT,0xFFB8D8F0);g.fill(middle,top,left+WIDTH,top+HEIGHT,0xFFF2C2D5);g.fill(left,top,left+4,halfHeight,0xFF3F7FC7);g.fill(left,halfHeight,left+4,top+HEIGHT,0xFFD86B9C);g.centeredText(font,title,width/2,top+18,0xFF2D2730);g.centeredText(font,Component.translatable("civitas.gender.prompt"),width/2,top+48,0xFF453B43);if(noticeTicks>0)g.centeredText(font,notice,width/2,top+122,noticeColor);super.extractRenderState(g,mouseX,mouseY,partialTick);}
}
