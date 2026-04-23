package com.crossinglines.planner;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class UndergroundFacilityPolicy implements FacilityPolicy {
    @Override
    public void decorate(ServerWorld world, BlockPos railPos, BlockPos prevPos, BlockPos nextPos, int index, int lightSpacing) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(railPos.add(dx, -1, dz), Blocks.STONE_BRICKS.getDefaultState());

                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos p = railPos.add(dx, dy, dz);
                    if (dx == 0 && dz == 0 && dy == 0) {
                        continue;
                    }
                    if (Math.abs(dx) == 1 || Math.abs(dz) == 1 || dy == 2) {
                        world.setBlockState(p, Blocks.STONE_BRICKS.getDefaultState());
                    } else {
                        world.setBlockState(p, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        if (lightSpacing > 0 && index % lightSpacing == 0) {
            world.setBlockState(railPos.up(2), Blocks.LANTERN.getDefaultState());
        }
    }
}
