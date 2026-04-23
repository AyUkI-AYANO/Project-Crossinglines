package com.crossinglines.planner;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class UndergroundFacilityPolicy implements FacilityPolicy {
    @Override
    public void decorate(ServerWorld world, BlockPos railPos, int index) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                BlockPos p = railPos.add(dx, dy, 0);
                if (Math.abs(dx) == 2 || dy == -1 || dy == 3) {
                    world.setBlockState(p, Blocks.STONE_BRICKS.getDefaultState());
                } else {
                    world.setBlockState(p, Blocks.AIR.getDefaultState());
                }
            }
        }

        if (index % 8 == 0) {
            world.setBlockState(railPos.up(2), Blocks.LANTERN.getDefaultState());
        }
    }
}
