package com.crossinglines.planner;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class SurfaceFacilityPolicy implements FacilityPolicy {
    @Override
    public void decorate(ServerWorld world, BlockPos railPos, BlockPos prevPos, BlockPos nextPos, int index, int lightSpacing) {
        BlockPos base = railPos.down();
        world.setBlockState(base, Blocks.STONE_BRICKS.getDefaultState());

        if (lightSpacing > 0 && index % lightSpacing == 0) {
            BlockPos direction = resolveDirection(railPos, prevPos, nextPos);
            BlockPos sideOffset = Math.abs(direction.getX()) >= Math.abs(direction.getZ())
                    ? new BlockPos(0, 0, (index / lightSpacing) % 2 == 0 ? 2 : -2)
                    : new BlockPos((index / lightSpacing) % 2 == 0 ? 2 : -2, 0, 0);
            BlockPos lampBase = railPos.add(sideOffset);
            world.setBlockState(lampBase, Blocks.COBBLESTONE_WALL.getDefaultState());
            world.setBlockState(lampBase.up(), Blocks.COBBLESTONE_WALL.getDefaultState());
            world.setBlockState(lampBase.up(2), Blocks.LANTERN.getDefaultState());
        }
    }

    private BlockPos resolveDirection(BlockPos railPos, BlockPos prevPos, BlockPos nextPos) {
        if (prevPos == null && nextPos == null) {
            return new BlockPos(1, 0, 0);
        }
        if (prevPos == null) {
            return new BlockPos(nextPos.getX() - railPos.getX(), 0, nextPos.getZ() - railPos.getZ());
        }
        if (nextPos == null) {
            return new BlockPos(railPos.getX() - prevPos.getX(), 0, railPos.getZ() - prevPos.getZ());
        }
        return new BlockPos(nextPos.getX() - prevPos.getX(), 0, nextPos.getZ() - prevPos.getZ());
    }
}
