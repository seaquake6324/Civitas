package com.seaquake6324.civitas.infrastructure.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class OpenCityManagementPayloadTest {
    @Test void roundTripsNamesAndExactIdentityFlags() {
        var founder=new OpenCityManagementPayload.PlayerEntry(UUID.randomUUID(),"青禾");
        var applicant=new OpenCityManagementPayload.PlayerEntry(UUID.randomUUID(),"River");
        var payload=new OpenCityManagementPayload(new BlockPos(1,64,2),"归港",0xC08040,
                true,true,true,true,List.of(founder),List.of(applicant),12,9,100,20,
                75,"EAST","WARNING",1200,2,1,0,300,4,1,0,6,2,12,35.5F,81F,42,"BORDER_EXPOSURE",12,true,64,500,3,2,1,4,2,
                List.of(new OpenCityManagementPayload.BuildingView(UUID.randomUUID(),"RESIDENCE","VALID",3,24,"RESIDENTIAL",6,2,1,1,1,true,3,"")),false,
                List.of(new OpenCityManagementPayload.MigrationView(UUID.randomUUID(),"Mira North",3,1,72.5F,1200,4)),
                new OpenCityManagementPayload.PopulationView(4,2,2,1,1,1,1,0,2,1,1,0,2,1,1,0,6,3,1,0,2,2,1,1,3,9876,75,80,55,10,List.of("BIRTH_FLOW_NOT_ACTIVE"),false),
                List.of(new OpenCityManagementPayload.OrphanView(UUID.randomUUID(),"Mila North",100,2)),
                List.of(new OpenCityManagementPayload.AdoptionView(UUID.randomUUID(),UUID.randomUUID(),"Mila North",2,1,true,1200,3)));
        RegistryFriendlyByteBuf buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            OpenCityManagementPayload.STREAM_CODEC.encode(buffer,payload);
            assertEquals(payload,OpenCityManagementPayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}
