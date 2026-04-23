package com.crossinglines.build;

import com.crossinglines.model.BuildStatus;
import com.crossinglines.model.RailLine;
import com.crossinglines.model.RailSettings;
import net.minecraft.block.BlockState;
import com.crossinglines.planner.FacilityPolicy;
import net.minecraft.block.DetectorRailBlock;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;

public final class BuildTaskQueue {
    private static final Deque<BuildTask> TASKS = new ArrayDeque<>();

    private BuildTaskQueue() {}

    public static void enqueue(ServerWorld world, RailLine line, FacilityPolicy policy, RailSettings settings) {
        TASKS.add(new BuildTask(world, line, policy, settings));
    }

    public static void tickServer(MinecraftServer server) {
        BuildTask task = TASKS.peekFirst();
        if (task == null) return;

        boolean done = task.tick();
        if (done) {
            TASKS.removeFirst();
        }
    }

    private static final class BuildTask {
        private final ServerWorld world;
        private final RailLine line;
        private final FacilityPolicy policy;
        private final RailSettings settings;
        private int cursor;

        private BuildTask(ServerWorld world, RailLine line, FacilityPolicy policy, RailSettings settings) {
            this.world = world;
            this.line = line;
            this.policy = policy;
            this.settings = settings;
            this.cursor = 0;
        }

        private boolean tick() {
            line.setStatus(BuildStatus.BUILDING);
            int max = settings.buildBlocksPerTick();
            int placed = 0;

            while (cursor < line.path().size() && placed < max) {
                BlockPos p = line.path().get(cursor);
                BlockPos prev = cursor > 0 ? line.path().get(cursor - 1) : null;
                BlockPos next = cursor + 1 < line.path().size() ? line.path().get(cursor + 1) : null;

                policy.decorate(world, p, prev, next, cursor, settings.lightSpacing());
                placeRail(p, cursor);

                cursor++;
                placed += 4;
            }

            if (cursor >= line.path().size()) {
                line.setStatus(BuildStatus.FINISHED);
                return true;
            }

            return false;
        }

        private void placeRail(BlockPos p, int index) {
            int cycle = index % 8;
            BlockState railState;

            if (cycle < 4) {
                world.setBlockState(p.down(), Blocks.STONE_BRICKS.getDefaultState());
                railState = Blocks.RAIL.getDefaultState();
            } else if (cycle == 4 || cycle == 7) {
                world.setBlockState(p.down(), Blocks.STONE_BRICKS.getDefaultState());
                railState = Blocks.DETECTOR_RAIL.getDefaultState().with(DetectorRailBlock.POWERED, false);
            } else {
                world.setBlockState(p.down(), Blocks.REDSTONE_BLOCK.getDefaultState());
                railState = Blocks.POWERED_RAIL.getDefaultState().with(PoweredRailBlock.POWERED, true);
            }

            world.setBlockState(p, railState);
        }
    }
}
