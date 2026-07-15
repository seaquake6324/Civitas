package com.seaquake6324.civitas.presentation.screen;

import com.seaquake6324.civitas.domain.CityName;
import com.seaquake6324.civitas.infrastructure.network.CityMembershipActionPayload;
import com.seaquake6324.civitas.infrastructure.network.OpenCityManagementPayload;
import com.seaquake6324.civitas.infrastructure.network.SubmitCityManagementPayload;
import com.seaquake6324.civitas.infrastructure.network.BeginBuildingRegistrationPayload;
import com.seaquake6324.civitas.infrastructure.network.BeginStorageAuthorizationPayload;
import com.seaquake6324.civitas.domain.building.BuildingPurpose;
import com.seaquake6324.civitas.infrastructure.network.MigrationDecisionPayload;
import com.seaquake6324.civitas.infrastructure.network.AdoptionActionPayload;
import com.seaquake6324.civitas.infrastructure.network.BeginPatrolRoutePayload;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Shared stone-backed six-page city core UI. Rules remain server-side. */
public final class CityCoreScreen extends Screen {
    private enum Page { OVERVIEW, POPULATION, MEMBERS, TERRITORY, BORDER, SECURITY, NEGLECT, BUILDINGS, IDENTITY }
    private static final int PANEL_WIDTH=440,PANEL_HEIGHT=252;
    private static final PageTheme[] THEMES={
            new PageTheme(0xFF000000,0),new PageTheme(0xFFFF8F9A,0),new PageTheme(0xFF5A3825,0),
            new PageTheme(0xFFC49A3A,0),new PageTheme(0xFF4F2A70,0),new PageTheme(0xFF2E7046,0),
            new PageTheme(0xFF777777,0),new PageTheme(0xFF62BCE8,0),new PageTheme(0xFFFFFFFF,0)};
    private record PageTheme(int primary,int secondary){boolean split(){return secondary!=0;}}
    private final OpenCityManagementPayload snapshot;
    private Page page=Page.OVERVIEW;
    private int left,top,selectedColor,noticeTicks,closeTicks;
    private EditBox nameBox,hexBox;
    private Button saveButton;
    private Component notice=Component.empty();
    private int noticeColor=0xFFFF7777;
    private boolean waiting;
    private int applicationOffset,residentOffset;

    public CityCoreScreen(OpenCityManagementPayload snapshot){
        super(Component.translatable("civitas.gui.manage_city"));this.snapshot=snapshot;selectedColor=snapshot.cityColor()&0xFFFFFF;
    }

    @Override public boolean isPauseScreen(){return false;}

    @Override protected void init(){
        left=(width-PANEL_WIDTH)/2;top=(height-PANEL_HEIGHT)/2;
        for(int i=0;i<Page.values().length;i++){int index=i;addRenderableWidget(Button.builder(Component.translatable("civitas.gui.page."+Page.values()[i].name().toLowerCase(Locale.ROOT)),b->{page=Page.values()[index];rebuildWidgets();}).bounds(left+8+i*47,top+28,45,18).build());}
        switch(page){
            case POPULATION->initPopulation();
            case MEMBERS->initMembers();
            case TERRITORY->initTerritory();
            case BUILDINGS->initBuildings();
            case IDENTITY->initIdentity();
            case SECURITY->initSecurity();
            default->{}
        }
    }

    private void initMembers(){
        int y=top+72;
        if(!snapshot.member())addRenderableWidget(Button.builder(Component.translatable("civitas.membership.apply"),b->membership(CityMembershipActionPayload.Action.APPLY,minecraft.player.getUUID())).bounds(left+120,y,140,20).build());
        else if(!snapshot.manager())addRenderableWidget(Button.builder(Component.translatable("civitas.membership.leave"),b->membership(CityMembershipActionPayload.Action.LEAVE,minecraft.player.getUUID())).bounds(left+120,y,140,20).build());
        if(!snapshot.manager())return;
        int shown=0;
        for(OpenCityManagementPayload.PlayerEntry entry:snapshot.applications().stream().skip(applicationOffset).limit(4).toList()){
            UUID target=entry.id();
            shown++;int rowY=y+(shown-1)*28;
            addRenderableWidget(Button.builder(Component.translatable("civitas.membership.approve"),b->membership(CityMembershipActionPayload.Action.APPROVE,target)).bounds(left+210,rowY,70,20).build());
            addRenderableWidget(Button.builder(Component.translatable("civitas.membership.reject"),b->membership(CityMembershipActionPayload.Action.REJECT,target)).bounds(left+286,rowY,70,20).build());
        }
        if(snapshot.applications().size()>4){addRenderableWidget(Button.builder(Component.literal("‹"),b->{applicationOffset=Math.max(0,applicationOffset-4);rebuildWidgets();}).bounds(left+22,top+49,22,18).build());addRenderableWidget(Button.builder(Component.literal("›"),b->{applicationOffset=Math.min(Math.max(0,snapshot.applications().size()-1),applicationOffset+4);rebuildWidgets();}).bounds(left+48,top+49,22,18).build());}
        shown=0;y=top+174;
        for(OpenCityManagementPayload.PlayerEntry entry:snapshot.residents().stream().skip(residentOffset).limit(2).toList()){
            UUID target=entry.id();
            shown++;int rowY=y+(shown-1)*25;
            addRenderableWidget(Button.builder(Component.translatable("civitas.membership.remove"),b->membership(CityMembershipActionPayload.Action.REMOVE,target)).bounds(left+286,rowY,70,20).build());
        }
        if(snapshot.residents().size()>2){addRenderableWidget(Button.builder(Component.literal("‹"),b->{residentOffset=Math.max(0,residentOffset-2);rebuildWidgets();}).bounds(left+214,top+222,22,18).build());addRenderableWidget(Button.builder(Component.literal("›"),b->{residentOffset=Math.min(Math.max(0,snapshot.residents().size()-1),residentOffset+2);rebuildWidgets();}).bounds(left+240,top+222,22,18).build());}
    }

    private void initTerritory(){
        if(snapshot.manager())addRenderableWidget(Button.builder(Component.translatable("civitas.expansion.begin"),b->{membership(CityMembershipActionPayload.Action.EXPAND,minecraft.player.getUUID());minecraft.popGuiLayer();}).bounds(left+120,top+190,140,20).build());
    }

    private void initSecurity(){if(snapshot.founder())addRenderableWidget(Button.builder(Component.translatable("civitas.patrol.begin"),b->{ClientPacketDistributor.sendToServer(new BeginPatrolRoutePayload(snapshot.corePos()));minecraft.popGuiLayer();}).bounds(left+120,top+180,180,20).build());}

    private void initPopulation(){
        if(snapshot.member()&&!snapshot.adoptions().isEmpty()){var adoption=snapshot.adoptions().getFirst();if(!adoption.selfConfirmed())addRenderableWidget(Button.builder(Component.translatable("civitas.adoption.confirm"),b->adoption(adoption.id(),adoption.revision(),AdoptionActionPayload.Action.CONFIRM)).bounds(left+286,top+170,64,20).build());addRenderableWidget(Button.builder(Component.translatable("civitas.adoption.decline"),b->adoption(adoption.id(),adoption.revision(),AdoptionActionPayload.Action.DECLINE)).bounds(left+354,top+170,64,20).build());}
        else if(snapshot.member()&&!snapshot.orphans().isEmpty()){var orphan=snapshot.orphans().getFirst();addRenderableWidget(Button.builder(Component.translatable("civitas.adoption.request"),b->adoption(orphan.childId(),orphan.revision(),AdoptionActionPayload.Action.REQUEST)).bounds(left+326,top+170,92,20).build());}
        if(snapshot.manager()&&!snapshot.migrationApplications().isEmpty()){var migration=snapshot.migrationApplications().getFirst();addRenderableWidget(Button.builder(Component.translatable("civitas.migration.approve"),b->migration(migration,true)).bounds(left+286,top+205,64,20).build());addRenderableWidget(Button.builder(Component.translatable("civitas.migration.reject"),b->migration(migration,false)).bounds(left+354,top+205,64,20).build());}
    }
    private void adoption(UUID target,long revision,AdoptionActionPayload.Action action){if(waiting)return;waiting=true;showNotice("civitas.gui.saving",true);ClientPacketDistributor.sendToServer(new AdoptionActionPayload(snapshot.corePos(),action,target,revision));}
    private void migration(OpenCityManagementPayload.MigrationView migration,boolean approve){if(waiting)return;waiting=true;showNotice("civitas.gui.saving",true);ClientPacketDistributor.sendToServer(new MigrationDecisionPayload(snapshot.corePos(),migration.id(),migration.revision(),approve));}

    private void initBuildings(){
        if(!snapshot.manager())return;
        int buildingRow=0;
        for(var building:snapshot.buildings().stream().limit(2).toList()){
            int y=top+68+buildingRow++*28;
            if(!"VALID".equals(building.status())||building.storageEndpoints()==0)continue;
            addRenderableWidget(Button.builder(Component.translatable("civitas.storage.select"),b->{
                ClientPacketDistributor.sendToServer(new BeginStorageAuthorizationPayload(snapshot.corePos(),building.id(),building.revision()));
                minecraft.popGuiLayer();
            }).bounds(left+354,y,66,18).build());
        }
        BuildingPurpose[] purposes=BuildingPurpose.values();
        for(int i=0;i<purposes.length;i++){
            BuildingPurpose purpose=purposes[i];int column=i%2,row=i/2;
            addRenderableWidget(Button.builder(Component.translatable("civitas.building.purpose."+purpose.name().toLowerCase(Locale.ROOT)),b->{
                ClientPacketDistributor.sendToServer(new BeginBuildingRegistrationPayload(snapshot.corePos(),purpose));
                minecraft.popGuiLayer();
            }).bounds(left+52+column*172,top+124+row*27,160,20).build());
        }
    }

    private void initIdentity(){
        nameBox=new EditBox(font,left+74,top+70,230,20,Component.translatable("civitas.gui.city_name"));nameBox.setMaxLength(40);nameBox.setValue(snapshot.cityName());nameBox.active=snapshot.manager();addRenderableWidget(nameBox);
        hexBox=new EditBox(font,left+250,top+177,82,20,Component.translatable("civitas.gui.hex_color"));hexBox.setMaxLength(7);hexBox.setValue(String.format(Locale.ROOT,"#%06X",selectedColor));hexBox.setResponder(this::onHexChanged);hexBox.active=snapshot.manager();addRenderableWidget(hexBox);
        saveButton=Button.builder(Component.translatable("civitas.gui.save_city"),b->submit()).bounds(left+120,top+216,140,20).build();saveButton.visible=snapshot.manager();addRenderableWidget(saveButton);
        if(snapshot.manager())setInitialFocus(nameBox);
    }

    private void membership(CityMembershipActionPayload.Action action,UUID target){if(waiting)return;waiting=true;showNotice("civitas.gui.saving",true);ClientPacketDistributor.sendToServer(new CityMembershipActionPayload(snapshot.corePos(),action,target));}
    private void onHexChanged(String text){String value=text.startsWith("#")?text.substring(1):text;if(value.matches("[0-9a-fA-F]{6}"))selectedColor=Integer.parseInt(value,16);}
    private boolean validHex(){return hexBox!=null&&hexBox.getValue().replaceFirst("^#","").matches("[0-9a-fA-F]{6}");}
    private void submit(){if(waiting)return;CityName.Validation name=CityName.validate(nameBox.getValue());if(!name.valid()){showNotice(name.errorKey(),false);return;}if(!validHex()){showNotice("civitas.gui.invalid_hex",false);return;}waiting=true;saveButton.active=false;showNotice("civitas.gui.saving",true);ClientPacketDistributor.sendToServer(new SubmitCityManagementPayload(snapshot.corePos(),name.normalized(),selectedColor));}
    public void handleResult(boolean success,String key){waiting=false;if(success){showNotice(key,true);closeTicks=24;}else{if(saveButton!=null)saveButton.active=true;showNotice(key,false);}}
    private void showNotice(String key,boolean neutral){notice=Component.translatable(key);noticeColor=neutral?tone(.68F):0xFFFF7777;noticeTicks=80;}
    @Override public void tick(){super.tick();if(noticeTicks>0)noticeTicks--;if(closeTicks>0&&--closeTicks==0)minecraft.popGuiLayer();}

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick){
        CivitasGuiTextures.stoneCityPanel(g,left,top,PANEL_WIDTH,PANEL_HEIGHT);drawTheme(g);
        g.centeredText(font,Component.literal(snapshot.cityName()+" · ").append(Component.translatable("civitas.gui.page."+page.name().toLowerCase(Locale.ROOT))),width/2,top+10,tone(.82F));
        switch(page){
            case OVERVIEW->overview(g);case POPULATION->population(g);case MEMBERS->members(g);case TERRITORY->territory(g);case BORDER->border(g);case SECURITY->security(g);case NEGLECT->neglect(g);case BUILDINGS->buildings(g);case IDENTITY->identity(g);
        }
        if(noticeTicks>0)g.centeredText(font,notice,width/2,top+238,noticeColor);super.extractRenderState(g,mouseX,mouseY,partialTick);
    }
    private void overview(GuiGraphicsExtractor g){g.text(font,Component.translatable("civitas.gui.city_summary",snapshot.residents().size(),snapshot.applications().size(),snapshot.territorySize(),snapshot.heartlandSize()),left+24,top+72,tone(.58F));g.text(font,Component.translatable(roleKey()),left+24,top+94,tone(.38F));}
    private void population(GuiGraphicsExtractor g){
        var p=snapshot.population();int x=left+18,y=top+48,line=15;
        g.text(font,Component.translatable("civitas.gui.population.total",p.total(),p.male(),p.female()),x,y,tone(.35F));y+=line;
        g.text(font,Component.translatable("civitas.gui.population.ages",p.child(),p.adolescent(),p.youngAdult(),p.matureAdult(),p.elder()),x,y,tone(.46F));y+=line;
        g.text(font,Component.translatable("civitas.gui.population.races",p.human(),p.pigfolk(),p.cowfolk(),p.sheepfolk()),x,y,tone(.55F));y+=line;
        g.text(font,Component.translatable("civitas.gui.population.runtime",p.virtualCount(),p.prewarming(),p.materialized(),p.locked()),x,y,tone(.58F));y+=line;
        g.text(font,Component.translatable("civitas.gui.population.housing",p.housingCapacity(),p.housed(),p.unassigned(),p.invalidResidence()),x,y,tone(.61F));y+=line;
        g.text(font,Component.translatable("civitas.gui.population.households",p.households(),p.partneredHouseholds(),p.householdsWithChildren(),p.employed()),x,y,tone(.66F));y+=line;
        g.text(font,Component.translatable("civitas.gui.population.coverage",String.format(Locale.ROOT,"%.1f",p.averageFoodCoverage()),String.format(Locale.ROOT,"%.1f",p.averageHousingCoverage()),String.format(Locale.ROOT,"%.1f",p.averageSettlementWillingness()),String.format(Locale.ROOT,"%.1f",p.averageMigrationWillingness())),x,y,tone(.71F));y+=line;
        g.text(font,Component.translatable("civitas.gui.population.deaths",p.permanentDeaths(),p.latestDeathAtTick()),x,y,tone(.75F));
        int lx=left+238,ly=top+48;g.text(font,Component.translatable("civitas.gui.population.flows_pending"),lx,ly,tone(.52F));ly+=12;g.text(font,Component.translatable("civitas.gui.population.flow_names"),lx,ly,tone(.57F));ly+=13;g.text(font,Component.translatable("civitas.gui.population.limitations"),lx,ly,tone(.62F));ly+=12;
        var visibleLimits=p.limitations().stream().filter(v->!v.equals("BIRTH_TREND_NOT_AVAILABLE")&&!v.equals("MIGRATION_TREND_NOT_AVAILABLE")&&!v.equals("OUT_MIGRATION_NOT_ACTIVE")).toList();for(String limitation:visibleLimits.stream().limit(4).toList()){g.text(font,Component.translatable("civitas.gui.population.limitation."+limitation.toLowerCase(Locale.ROOT)),lx,ly,tone(.67F));ly+=12;}
        if(visibleLimits.size()>4)g.text(font,Component.translatable("civitas.gui.population.more_limitations",visibleLimits.size()-4),lx,ly,0xFFFFC477);
        if(!snapshot.adoptions().isEmpty()){var a=snapshot.adoptions().getFirst();g.text(font,Component.translatable("civitas.gui.population.adoption_pending",a.childName(),a.confirmedPlayers(),a.requiredPlayers(),a.remaining()/20),x,top+174,tone(.80F));}
        else if(!snapshot.orphans().isEmpty()){var o=snapshot.orphans().getFirst();g.text(font,Component.translatable("civitas.gui.population.orphan",o.name()),x,top+174,tone(.80F));}
        else g.text(font,Component.translatable("civitas.gui.population.no_orphans"),x,top+174,tone(.80F));
        if(!snapshot.migrationApplications().isEmpty()){var m=snapshot.migrationApplications().getFirst();g.text(font,Component.translatable("civitas.gui.population.migrant",m.names(),m.members(),m.children(),String.format(Locale.ROOT,"%.1f",m.attraction()),m.decisionRemaining()/20),x,top+209,tone(.84F));}
        else g.text(font,Component.translatable("civitas.gui.population.no_migrants"),x,top+209,tone(.84F));
        if(p.truncated())g.text(font,Component.translatable("civitas.gui.population.truncated"),x,top+230,0xFFFFC477);
    }
    private void members(GuiGraphicsExtractor g){int y=top+60;if(snapshot.manager()){int i=0;for(OpenCityManagementPayload.PlayerEntry entry:snapshot.applications().stream().skip(applicationOffset).limit(4).toList())g.text(font,Component.literal(entry.name()),left+24,y+i++*28+6,tone(.48F));y=top+174;i=0;for(OpenCityManagementPayload.PlayerEntry entry:snapshot.residents().stream().skip(residentOffset).limit(2).toList())g.text(font,Component.literal(entry.name()),left+24,y+i++*25+6,tone(.65F));}}
    private void territory(GuiGraphicsExtractor g){g.text(font,Component.translatable("civitas.gui.territory.count",snapshot.territorySize(),snapshot.heartlandSize()),left+24,top+72,tone(.45F));g.text(font,Component.translatable("civitas.gui.territory.cooldown",snapshot.expansionCooldownRemaining()/20),left+24,top+94,tone(.62F));g.text(font,Component.translatable("civitas.gui.territory.rules"),left+24,top+116,tone(.76F));}
    private void border(GuiGraphicsExtractor g){g.text(font,Component.translatable("civitas.gui.border.summary",String.format(Locale.ROOT,"%.1f",snapshot.maxPressure()),snapshot.threatDirection(),snapshot.threatPhase()),left+24,top+72,tone(.34F));g.text(font,Component.translatable("civitas.gui.border.remaining",snapshot.threatPhaseRemaining()/20),left+24,top+94,tone(.52F));g.text(font,Component.translatable("civitas.gui.border.help"),left+24,top+116,tone(.75F));}
    private void security(GuiGraphicsExtractor g){if(snapshot.securityCells()==0){g.text(font,Component.translatable("civitas.gui.security.pending"),left+24,top+72,tone(.55F));return;}var weak=com.seaquake6324.civitas.domain.ChunkCoordinate.unpack(snapshot.securityWeakestChunk());g.text(font,Component.translatable("civitas.gui.security.summary",snapshot.securityCells(),String.format(Locale.ROOT,"%.1f",snapshot.securityAverageRisk()),String.format(Locale.ROOT,"%.1f",snapshot.securityMaxRisk())),left+24,top+72,tone(.36F));g.text(font,Component.translatable("civitas.gui.security.weakest",weak.x(),weak.z(),Component.translatable("civitas.security.factor."+snapshot.securityPrimaryFactor().toLowerCase(Locale.ROOT))),left+24,top+94,tone(.52F));g.text(font,Component.translatable("civitas.gui.security.missing",snapshot.securityMissingCells()),left+24,top+116,tone(.66F));g.text(font,Component.translatable("civitas.gui.security.coverage",snapshot.recentlyPatrolledCells(),snapshot.visibleGuardCells()),left+24,top+138,tone(.74F));g.text(font,Component.translatable("civitas.gui.security.guards",snapshot.activeGuards(),snapshot.guardAssignments(),snapshot.patrolRoutes()),left+24,top+160,tone(.82F));if(snapshot.securityTruncated())g.text(font,Component.translatable("civitas.gui.security.truncated",snapshot.securityExamined()),left+24,top+182,0xFFFFC477);}
    private void neglect(GuiGraphicsExtractor g){g.text(font,Component.translatable("civitas.gui.neglect.summary",snapshot.warningChunks(),snapshot.abandonedChunks(),snapshot.retractableChunks()),left+24,top+72,tone(.34F));g.text(font,Component.translatable("civitas.gui.neglect.recovery",snapshot.recoveryRemaining()),left+24,top+96,tone(.55F));g.text(font,Component.translatable("civitas.gui.neglect.help"),left+24,top+120,tone(.76F));}
    private void buildings(GuiGraphicsExtractor g){g.text(font,Component.translatable("civitas.gui.buildings.summary",snapshot.validBuildings(),snapshot.staleBuildings(),snapshot.invalidBuildings()),left+18,top+54,tone(.38F));g.text(font,Component.translatable("civitas.gui.buildings.capacity",snapshot.housingCapacity(),snapshot.guardCapacity()),left+238,top+54,tone(.55F));int y=top+70;for(var building:snapshot.buildings().stream().limit(2).toList()){Component purpose=Component.translatable("civitas.building.purpose."+building.purpose().toLowerCase(Locale.ROOT));Component status=Component.translatable("civitas.building.status."+building.status().toLowerCase(Locale.ROOT));Component facility=Component.translatable("civitas.building.facility."+building.requiredFacility().toLowerCase(Locale.ROOT));Component reason=building.invalidReason().isBlank()?Component.translatable("civitas.building.reason.none"):Component.translatable("civitas.building.failure."+building.invalidReason().toLowerCase(Locale.ROOT));g.text(font,Component.translatable("civitas.gui.buildings.row",purpose,status,building.capacity(),building.cells(),facility,building.requiredFacilityCount(),building.revision(),reason),left+18,y,tone(.65F));g.text(font,Component.translatable("civitas.gui.buildings.features",building.boundaryPorts(),building.workstations(),building.authorizedStorageEndpoints(),building.storageEndpoints(),building.entranceConnected()),left+34,y+13,tone(.76F));y+=28;}if(snapshot.buildings().isEmpty())g.text(font,Component.translatable("civitas.gui.buildings.empty"),left+18,y,tone(.72F));if(snapshot.buildingsTruncated()||snapshot.buildings().size()>2)g.text(font,Component.translatable("civitas.gui.buildings.truncated"),left+18,top+211,0xFFFFC477);}
    private void identity(GuiGraphicsExtractor g){g.text(font,Component.translatable("civitas.gui.city_name"),left+24,top+76,tone(.55F));g.text(font,Component.translatable("civitas.gui.city_color"),left+24,top+104,tone(.55F));drawSpectrum(g);g.fill(left+214,top+177,left+236,top+197,0xFF000000|selectedColor);g.outline(left+214,top+177,22,20,tone(.40F));}
    private void drawSpectrum(GuiGraphicsExtractor g){int x0=left+74,y0=top+108;for(int x=0;x<46;x++)for(int y=0;y<10;y++){float hue=x/46F,p=y/9F,s=p<.5F?p*2:1,b=p<.5F?1:1-(p-.5F)*1.6F;g.fill(x0+x*5,y0+y*6,x0+x*5+5,y0+y*6+6,0xFF000000|(java.awt.Color.HSBtoRGB(hue,s,b)&0xFFFFFF));}g.outline(x0,y0,230,60,0xFF6E6254);}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){if(page==Page.IDENTITY){int x0=left+74,y0=top+108;if(event.button()==0&&event.x()>=x0&&event.x()<x0+230&&event.y()>=y0&&event.y()<y0+60){float hue=(float)(event.x()-x0)/230,p=(float)(event.y()-y0)/60,s=p<.5F?p*2:1,b=p<.5F?1:1-(p-.5F)*1.6F;selectedColor=java.awt.Color.HSBtoRGB(hue,s,b)&0xFFFFFF;hexBox.setValue(String.format(Locale.ROOT,"#%06X",selectedColor));return true;}}return super.mouseClicked(event,doubleClick);}
    private String roleKey(){if(snapshot.founder()&&snapshot.lord())return "civitas.gui.role.founder_lord";if(snapshot.founder())return "civitas.gui.role.founder";if(snapshot.lord())return "civitas.gui.role.lord";return snapshot.member()?"civitas.gui.role.member":"civitas.gui.role.outsider";}
    private void drawTheme(GuiGraphicsExtractor g){PageTheme theme=THEMES[page.ordinal()];if(theme.split()){int halfHeight=top+PANEL_HEIGHT/2;g.fill(left,top,left+4,halfHeight,theme.primary());g.fill(left,halfHeight,left+4,top+PANEL_HEIGHT,theme.secondary());}else g.fill(left,top,left+4,top+PANEL_HEIGHT,theme.primary());}
    private int tone(float whiteMix){int color=THEMES[page.ordinal()].primary(),r=(color>>16)&255,g=(color>>8)&255,b=color&255;r=Math.round(r+(255-r)*whiteMix);g=Math.round(g+(255-g)*whiteMix);b=Math.round(b+(255-b)*whiteMix);return 0xFF000000|(r<<16)|(g<<8)|b;}
}
