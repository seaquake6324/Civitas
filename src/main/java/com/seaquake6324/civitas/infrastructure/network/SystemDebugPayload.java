package com.seaquake6324.civitas.infrastructure.network;

import com.seaquake6324.civitas.CivitasMod;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Bounded server-authored metrics for the system pages of the opt-in debug overlay. */
public record SystemDebugPayload(boolean enabled,List<PageData> pages) implements CustomPacketPayload{
    private static final int PAGE_CAP=7,LINE_CAP=8,TEXT_CAP=240;
    public static final Type<SystemDebugPayload>TYPE=new Type<>(Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID,"system_debug"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SystemDebugPayload>STREAM_CODEC=CustomPacketPayload.codec(SystemDebugPayload::write,SystemDebugPayload::read);
    public SystemDebugPayload{pages=pages==null?List.of():List.copyOf(pages.stream().limit(PAGE_CAP).toList());}
    public static SystemDebugPayload disabled(){return new SystemDebugPayload(false,List.of());}
    private void write(RegistryFriendlyByteBuf b){b.writeBoolean(enabled);if(!enabled)return;b.writeVarInt(pages.size());for(PageData page:pages)page.write(b);}
    private static SystemDebugPayload read(RegistryFriendlyByteBuf b){if(!b.readBoolean())return disabled();int count=Math.min(PAGE_CAP,Math.max(0,b.readVarInt()));java.util.ArrayList<PageData>pages=new java.util.ArrayList<>(count);for(int i=0;i<count;i++)pages.add(PageData.read(b));return new SystemDebugPayload(true,pages);}
    @Override public Type<? extends CustomPacketPayload>type(){return TYPE;}
    public record PageData(String id,List<String>lines){
        public PageData{id=id==null?"":id;lines=lines==null?List.of():List.copyOf(lines.stream().limit(LINE_CAP).map(v->v.length()>TEXT_CAP?v.substring(0,TEXT_CAP):v).toList());}
        private void write(RegistryFriendlyByteBuf b){b.writeUtf(id,32);b.writeVarInt(lines.size());for(String line:lines)b.writeUtf(line,TEXT_CAP);}
        private static PageData read(RegistryFriendlyByteBuf b){String id=b.readUtf(32);int count=Math.min(LINE_CAP,Math.max(0,b.readVarInt()));java.util.ArrayList<String>lines=new java.util.ArrayList<>(count);for(int i=0;i<count;i++)lines.add(b.readUtf(TEXT_CAP));return new PageData(id,lines);}
    }
}
