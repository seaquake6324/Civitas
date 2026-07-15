package com.seaquake6324.civitas.infrastructure.border;

import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.border.BorderFortificationEvidence;
import com.seaquake6324.civitas.domain.territory.TerritoryTopology;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;

/** Exactly sixteen surface columns and five local block checks per edge. */
public final class BoundedBorderScanner {
    public static final int MAX_BLOCK_CHECKS=80;
    public static BorderFortificationEvidence scan(ServerLevel level,long packedChunk,TerritoryTopology.Direction edge){
        ChunkCoordinate chunk=ChunkCoordinate.unpack(packedChunk);int minX=chunk.x()<<4,minZ=chunk.z()<<4;
        boolean[] barriers=new boolean[16];int entrances=0,gaps=0,gates=0,paths=0;
        for(int i=0;i<16;i++){
            int x=switch(edge){case EAST->minX+15;case WEST->minX;default->minX+i;};
            int z=switch(edge){case SOUTH->minZ+15;case NORTH->minZ;default->minZ+i;};
            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            BlockState at=level.getBlockState(new BlockPos(x,y,z));BlockState above=level.getBlockState(new BlockPos(x,y+1,z));
            barriers[i]=barrier(at)||barrier(above);boolean gate=gate(at)||gate(above);if(gate){entrances++;if(open(at)||open(above))gates++;}
            if(!barriers[i]&&!gate)gaps++;
            int insideX=x-edgeDx(edge),insideZ=z-edgeDz(edge);int insideY=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,insideX,insideZ);
            BlockPos feet=new BlockPos(insideX,insideY,insideZ);if(level.getBlockState(feet).isAir()&&level.getBlockState(feet.above()).isAir()&&!level.getBlockState(feet.below()).isAir())paths++;
        }
        int continuous=0,run=0;for(int i=0;i<=16;i++){if(i<16&&barriers[i])run++;else{if(run>=2)continuous+=run;run=0;}}
        return BorderFortificationEvidence.calculate(16,continuous,entrances,gaps,gates,paths,new BorderFortificationEvidence.Weights(CivitasConfig.FORT_WALL_WEIGHT.get(),CivitasConfig.FORT_ENTRANCE_WEIGHT.get(),CivitasConfig.FORT_GATE_WEIGHT.get(),CivitasConfig.FORT_PATH_WEIGHT.get(),CivitasConfig.FORT_GAP_PENALTY.get()));
    }
    private static boolean barrier(BlockState state){return state.getBlock() instanceof WallBlock||state.getBlock() instanceof FenceBlock||state.getBlock() instanceof IronBarsBlock;}
    private static boolean gate(BlockState state){return state.getBlock() instanceof FenceGateBlock;}
    private static boolean open(BlockState state){return state.hasProperty(BlockStateProperties.OPEN)&&state.getValue(BlockStateProperties.OPEN);}
    private static int edgeDx(TerritoryTopology.Direction d){return d==TerritoryTopology.Direction.EAST?1:d==TerritoryTopology.Direction.WEST?-1:0;}
    private static int edgeDz(TerritoryTopology.Direction d){return d==TerritoryTopology.Direction.SOUTH?1:d==TerritoryTopology.Direction.NORTH?-1:0;}
    private BoundedBorderScanner(){}
}
