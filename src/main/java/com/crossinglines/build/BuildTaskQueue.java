package com.crossinglines.build;

import com.crossinglines.model.BuildStatus;
import com.crossinglines.model.RailLine;
import com.crossinglines.model.RailSettings;
import com.crossinglines.planner.FacilityPolicy;
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

                world.setBlockState(p.down(), Blocks.STONE_BRICKS.getDefaultState());
                world.setBlockState(p, Blocks.RAIL.getDefaultState());
                policy.decorate(world, p, cursor);

                cursor++;
                placed += 4;
            }

            if (cursor >= line.path().size()) {
                line.setStatus(BuildStatus.FINISHED);
                return true;
            }

            return false;
        }
    }
}
