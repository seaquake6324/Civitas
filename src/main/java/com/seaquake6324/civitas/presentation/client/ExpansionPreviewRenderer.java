package com.seaquake6324.civitas.presentation.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import com.seaquake6324.civitas.domain.ChunkCoordinate;

public final class ExpansionPreviewRenderer implements DebugRenderer.SimpleDebugRenderer {
    @Override public void emitGizmos(double cameraX,double cameraY,double cameraZ,DebugValueAccess values,Frustum frustum,float partialTick) {
        var player=Minecraft.getInstance().player;if(player==null)return;double y=player.getY()+.08;int rendered=0;
        for(long packed:ExpansionPreview.candidates()){
            ChunkCoordinate chunk=ChunkCoordinate.unpack(packed);double centerX=(chunk.x()<<4)+8,centerZ=(chunk.z()<<4)+8;
            if(Math.abs(centerX-player.getX())>96||Math.abs(centerZ-player.getZ())>96||rendered++>=128)continue;
            boolean eligible=ExpansionPreview.eligible(packed);int stroke=eligible?0xDD65C985:0xDDD66F72;int fill=eligible?0x3365C985:0x33D66F72;
            int minX=chunk.x()<<4,minZ=chunk.z()<<4;
            Gizmos.cuboid(new AABB(minX,y,minZ,minX+16,y+.12,minZ+16),GizmoStyle.strokeAndFill(stroke,2F,fill));
        }
    }
}
