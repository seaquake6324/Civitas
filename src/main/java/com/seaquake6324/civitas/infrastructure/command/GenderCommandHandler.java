package com.seaquake6324.civitas.infrastructure.command;

import com.seaquake6324.civitas.application.ChangeCitizenGenderService;
import com.seaquake6324.civitas.application.SelectPlayerGenderService;
import com.seaquake6324.civitas.domain.population.CitizenRecord;
import com.seaquake6324.civitas.domain.population.Gender;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/** Administrator-only adapter; normal gameplay has no gender mutation path. */
public final class GenderCommandHandler {
    private static final SelectPlayerGenderService PLAYERS=new SelectPlayerGenderService();
    private static final ChangeCitizenGenderService CITIZENS=new ChangeCitizenGenderService();
    public static int change(CommandSourceStack source,Gender gender,String target){PopulationSavedData data=PopulationSavedData.get(source.getServer());if(data.readOnly())return result(source,false,"人口存档处于未来 schema 只读保护");
        String normalized=target.trim();UUID id=parseUuid(normalized);if(id!=null&&data.citizen(id).isPresent()){CitizenRecord changed=CITIZENS.adminOverride(data,id,gender);return result(source,true,"NPC "+changed.displayName()+" 性别已改为 "+gender);}
        List<CitizenRecord> named=data.citizensByName(normalized,2);if(named.size()>1)return result(source,false,"居民姓名不唯一，请改用 UUID");if(named.size()==1){CitizenRecord changed=CITIZENS.adminOverride(data,named.getFirst().id(),gender);return result(source,true,"NPC "+changed.displayName()+" 性别已改为 "+gender);}
        var online=source.getServer().getPlayerList().getPlayerByName(normalized);if(online!=null){PLAYERS.adminOverride(data,online.getUUID(),gender);return result(source,true,"玩家 "+online.getGameProfile().name()+" 性别已改为 "+gender);}
        var cached=source.getServer().services().nameToIdCache().get(normalized);if(cached.isPresent()){PLAYERS.adminOverride(data,cached.get().id(),gender);return result(source,true,"玩家 "+cached.get().name()+" 性别已改为 "+gender);}
        return result(source,false,"未找到玩家或居民："+normalized);
    }
    private static UUID parseUuid(String value){try{return UUID.fromString(value);}catch(IllegalArgumentException exception){return null;}}
    private static int result(CommandSourceStack source,boolean success,String text){if(source.getPlayer()!=null)source.getPlayer().sendSystemMessage(Component.literal(text),true);return success?1:0;}
    private GenderCommandHandler(){}
}
