package com.crossinglines.path;

import com.crossinglines.model.RailSettings;
import com.crossinglines.model.RailType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.*;

public class TerrainPathFinder {
    private record Node(BlockPos pos, double g, double f, Node parent) {}

    public List<BlockPos> findPath(ServerWorld world, BlockPos start, BlockPos end, RailType type, RailSettings settings) {
        Integer undergroundY = resolveUndergroundY(world, start, end, type, settings);
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
        Map<BlockPos, Double> bestG = new HashMap<>();

        BlockPos startPos = normalize(world, start, type, settings, undergroundY);
        BlockPos endPos = normalize(world, end, type, settings, undergroundY);

        Node startNode = new Node(startPos, 0, heuristic(startPos, endPos), null);
        open.add(startNode);
        bestG.put(startPos, 0.0);

        int maxIterations = 80_000;
        int iterations = 0;

        while (!open.isEmpty() && iterations++ < maxIterations) {
            Node current = open.poll();
            if (current.pos().isWithinDistance(endPos, 1.5)) {
                return reconstruct(current);
            }

            for (BlockPos next : neighbors(world, current, type, settings, undergroundY)) {
                double tentativeG = current.g() + moveCost(current.pos(), next, type);
                if (tentativeG >= bestG.getOrDefault(next, Double.POSITIVE_INFINITY)) continue;

                bestG.put(next, tentativeG);
                double f = tentativeG + heuristic(next, endPos);
                open.add(new Node(next, tentativeG, f, current));
            }
        }

        return List.of();
    }

    private List<BlockPos> neighbors(ServerWorld world, Node current, RailType type, RailSettings settings, Integer undergroundY) {
        List<BlockPos> result = new ArrayList<>(4);
        BlockPos pos = current.pos();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] d : dirs) {
            int nx = pos.getX() + d[0];
            int nz = pos.getZ() + d[1];
            int ny = type == RailType.UNDERGROUND && undergroundY != null
                    ? undergroundY
                    : world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, nx, nz);

            int slope = Math.abs(ny - pos.getY());
            if (slope > settings.maxSlopePerStep()) continue;

            BlockPos next = new BlockPos(nx, ny, nz);
            if (violatesTurnBeforeClimb(current, next)) continue;

            result.add(next);
        }
        return result;
    }

    private boolean violatesTurnBeforeClimb(Node current, BlockPos next) {
        if (next.getY() == current.pos().getY()) {
            return false;
        }
        if (current.parent() == null || current.parent().parent() == null) {
            return false;
        }

        BlockPos prev = current.parent().pos();
        BlockPos curr = current.pos();
        int dx1 = Integer.signum(curr.getX() - prev.getX());
        int dz1 = Integer.signum(curr.getZ() - prev.getZ());
        int dx2 = Integer.signum(next.getX() - curr.getX());
        int dz2 = Integer.signum(next.getZ() - curr.getZ());

        return dx1 != dx2 || dz1 != dz2;
    }

    private double moveCost(BlockPos a, BlockPos b, RailType type) {
        int dy = Math.abs(a.getY() - b.getY());
        double cost = 1.0 + dy * 2.5;
        if (type == RailType.UNDERGROUND) {
            cost += 0.3;
        }
        return cost;
    }

    private double heuristic(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }

    private List<BlockPos> reconstruct(Node end) {
        LinkedList<BlockPos> path = new LinkedList<>();
        Node cursor = end;
        while (cursor != null) {
            path.addFirst(cursor.pos());
            cursor = cursor.parent();
        }
        return path;
    }

    private BlockPos normalize(ServerWorld world, BlockPos pos, RailType type, RailSettings settings, Integer undergroundY) {
        int y = type == RailType.UNDERGROUND && undergroundY != null
                ? undergroundY
                : world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    private Integer resolveUndergroundY(ServerWorld world, BlockPos start, BlockPos end, RailType type, RailSettings settings) {
        if (type != RailType.UNDERGROUND) {
            return null;
        }

        int startSurfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, start.getX(), start.getZ());
        int endSurfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, end.getX(), end.getZ());
        int baseSurfaceY = Math.min(startSurfaceY, endSurfaceY);
        int minBuildY = world.getBottomY() + 5;
        return Math.max(minBuildY, baseSurfaceY - Math.max(4, settings.undergroundDepth()));
    }
}
