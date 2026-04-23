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
        private int cursor;

        private BuildTask(ServerWorld world, RailLine line, FacilityPolicy policy, RailSettings settings) {
            this.world = world;
            this.line = line;
            this.policy = policy;
            this.settings = settings;
            this.path = line.path();
            this.railPositions = new HashSet<>(line.path());
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

            boolean turn = isTurn(index);
            boolean isDBD = canPlaceDetectorBoostDetector(index);
            BlockState railState = Blocks.RAIL.getDefaultState();

            if (!turn && isDBD) {
                int phase = index % 8;
                if (phase == 4 || phase == 6) {
                    railState = Blocks.DETECTOR_RAIL.getDefaultState().with(DetectorRailBlock.POWERED, false);
                    world.setBlockState(p.down(), Blocks.STONE_BRICKS.getDefaultState());
                } else if (phase == 5) {
                    railState = Blocks.POWERED_RAIL.getDefaultState().with(PoweredRailBlock.POWERED, true);
                    world.setBlockState(p.down(), Blocks.REDSTONE_BLOCK.getDefaultState());
                } else {
                    world.setBlockState(p.down(), Blocks.STONE_BRICKS.getDefaultState());
                }
            } else {
                world.setBlockState(p.down(), Blocks.STONE_BRICKS.getDefaultState());
            }

            world.setBlockState(p, railState);
        }

        private boolean canPlaceDetectorBoostDetector(int index) {
            int phase = index % 8;
            if (phase < 4 || phase > 6) {
                return false;
            }

            int moduleStart = index - phase + 4;
            if (moduleStart + 2 >= path.size()) {
                return false;
            }

            return isStraight(moduleStart)
                    && isStraight(moduleStart + 1)
                    && isStraight(moduleStart + 2)
                    && isFlat(moduleStart, moduleStart + 1)
                    && isFlat(moduleStart + 1, moduleStart + 2);
        }

        private boolean isTurn(int index) {
            return !isStraight(index);
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

        private boolean isFlat(int i, int j) {
            return path.get(i).getY() == path.get(j).getY();
        }
    }
}
