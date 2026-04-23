package com.crossinglines.build;

import com.crossinglines.model.BuildStatus;
import com.crossinglines.model.RailLine;
import com.crossinglines.model.RailSettings;
import net.minecraft.block.BlockState;
import com.crossinglines.planner.FacilityPolicy;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        private final List<BlockPos> path;
        private final Set<BlockPos> railPositions;
        private int poweredModuleRemaining;
        private int lastModuleStart;
        private int cursor;

        private BuildTask(ServerWorld world, RailLine line, FacilityPolicy policy, RailSettings settings) {
            this.world = world;
            this.line = line;
            this.policy = policy;
            this.settings = settings;
            this.path = line.path();
            this.railPositions = new HashSet<>(line.path());
            this.poweredModuleRemaining = 0;
            this.lastModuleStart = Integer.MIN_VALUE / 2;
            this.cursor = 0;
        }

        private boolean tick() {
            line.setStatus(BuildStatus.BUILDING);
            int max = settings.buildBlocksPerTick();
            int placed = 0;

            while (cursor < path.size() && placed < max) {
                BlockPos p = path.get(cursor);
                BlockPos prev = cursor > 0 ? path.get(cursor - 1) : null;
                BlockPos next = cursor + 1 < path.size() ? path.get(cursor + 1) : null;

                policy.decorate(world, p, prev, next, cursor, settings.lightSpacing(), railPositions);
                placeRail(p, cursor);

                cursor++;
                placed += 4;
            }

            if (cursor >= path.size()) {
                line.setStatus(BuildStatus.FINISHED);
                return true;
            }

            return false;
        }

        private void placeRail(BlockPos p, int index) {
            world.setBlockState(p, Blocks.AIR.getDefaultState());

            BlockState railState = Blocks.RAIL.getDefaultState();

            if (shouldPlacePoweredRail(index)) {
                railState = Blocks.POWERED_RAIL.getDefaultState().with(PoweredRailBlock.POWERED, true);
                world.setBlockState(p.down(), Blocks.REDSTONE_BLOCK.getDefaultState());
            } else {
                world.setBlockState(p.down(), Blocks.STONE_BRICKS.getDefaultState());
            }

            world.setBlockState(p, railState);
        }

        private boolean shouldPlacePoweredRail(int index) {
            if (index <= 0 || index >= path.size() - 1) {
                poweredModuleRemaining = 0;
                return false;
            }

            if (poweredModuleRemaining > 0) {
                poweredModuleRemaining--;
                return true;
            }

            int minGap = isStraight(index) ? 8 : 4;
            if (index - lastModuleStart < minGap) {
                return false;
            }

            int moduleLength = resolveModuleLength(index);
            if (moduleLength <= 0) {
                return false;
            }

            lastModuleStart = index;
            poweredModuleRemaining = moduleLength - 1;
            return true;
        }

        private int resolveModuleLength(int startIndex) {
            int straightLength = countContinuous(startIndex, true);
            if (straightLength > 0) {
                return straightLength;
            }

            int curveLength = countContinuous(startIndex, false);
            return Math.max(1, curveLength);
        }

        private int countContinuous(int startIndex, boolean straight) {
            int length = 0;
            for (int i = startIndex; i < path.size() - 1 && length < 3; i++) {
                if (isStraight(i) != straight) {
                    break;
                }
                length++;
            }
            return length;
        }

        private boolean isStraight(int index) {
            if (index <= 0 || index >= path.size() - 1) {
                return true;
            }

            BlockPos prev = path.get(index - 1);
            BlockPos curr = path.get(index);
            BlockPos next = path.get(index + 1);
            int dx1 = Integer.signum(curr.getX() - prev.getX());
            int dz1 = Integer.signum(curr.getZ() - prev.getZ());
            int dx2 = Integer.signum(next.getX() - curr.getX());
            int dz2 = Integer.signum(next.getZ() - curr.getZ());
            return dx1 == dx2 && dz1 == dz2;
        }

    }
}
