package com.crossinglines.planner;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface FacilityPolicy {
    void decorate(ServerWorld world, BlockPos railPos, BlockPos prevPos, BlockPos nextPos, int index, int lightSpacing);
}
