package com.crossinglines.planner;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class SurfaceFacilityPolicy implements FacilityPolicy {
    @Override
    public void decorate(ServerWorld world, BlockPos railPos, int index) {
        BlockPos base = railPos.down();
        world.setBlockState(base, Blocks.STONE_BRICKS.getDefaultState());

        if (index % 10 == 0) {
            BlockPos lampBase = railPos.add(2, 0, 0);
            world.setBlockState(lampBase, Blocks.COBBLESTONE_WALL.getDefaultState());
            world.setBlockState(lampBase.up(), Blocks.COBBLESTONE_WALL.getDefaultState());
            world.setBlockState(lampBase.up(2), Blocks.LANTERN.getDefaultState());
        }
    }
}
