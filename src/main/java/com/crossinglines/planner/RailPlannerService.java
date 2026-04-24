package com.crossinglines.planner;

import com.crossinglines.model.BuildStatus;
import com.crossinglines.model.RailLine;
import com.crossinglines.model.RailSettings;
import com.crossinglines.model.RailType;
import com.crossinglines.path.TerrainPathFinder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

public class RailPlannerService {
    private final TerrainPathFinder pathFinder = new TerrainPathFinder();

    public RailLine plan(ServerWorld world, BlockPos start, BlockPos end, RailType type, RailSettings settings) {
        List<BlockPos> path = pathFinder.findPath(world, start, end, settings);
        return new RailLine(
                UUID.randomUUID(),
                "Line-" + System.currentTimeMillis(),
                start,
                end,
                type,
                path,
                BuildStatus.PLANNED
        );
    }

    public FacilityPolicy policyOf(RailType type) {
        return type == RailType.SURFACE ? new SurfaceFacilityPolicy() : new EmbankmentFacilityPolicy();
    }
}
