package com.crossinglines.model;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RailLine {
    private final UUID id;
    private String name;
    private final BlockPos start;
    private final BlockPos end;
    private final RailType type;
    private final List<BlockPos> path;
    private BuildStatus status;

    public RailLine(UUID id, String name, BlockPos start, BlockPos end, RailType type, List<BlockPos> path, BuildStatus status) {
        this.id = id;
        this.name = name;
        this.start = start;
        this.end = end;
        this.type = type;
        this.path = path;
        this.status = status;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public BlockPos start() { return start; }
    public BlockPos end() { return end; }
    public RailType type() { return type; }
    public List<BlockPos> path() { return path; }
    public BuildStatus status() { return status; }

    public void setStatus(BuildStatus status) {
        this.status = status;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("id", id);
        nbt.putString("name", name);
        nbt.putString("type", type.name());
        nbt.putString("status", status.name());
        nbt.putInt("sx", start.getX());
        nbt.putInt("sy", start.getY());
        nbt.putInt("sz", start.getZ());
        nbt.putInt("ex", end.getX());
        nbt.putInt("ey", end.getY());
        nbt.putInt("ez", end.getZ());

        NbtList nbtPath = new NbtList();
        for (BlockPos pos : path) {
            NbtCompound p = new NbtCompound();
            p.putInt("x", pos.getX());
            p.putInt("y", pos.getY());
            p.putInt("z", pos.getZ());
            nbtPath.add(p);
        }
        nbt.put("path", nbtPath);
        return nbt;
    }

    public static RailLine fromNbt(NbtCompound nbt) {
        UUID id = nbt.getUuid("id");
        String name = nbt.getString("name");
        RailType type = RailType.valueOf(nbt.getString("type"));
        BuildStatus status = BuildStatus.valueOf(nbt.getString("status"));

        BlockPos start = new BlockPos(nbt.getInt("sx"), nbt.getInt("sy"), nbt.getInt("sz"));
        BlockPos end = new BlockPos(nbt.getInt("ex"), nbt.getInt("ey"), nbt.getInt("ez"));

        List<BlockPos> path = new ArrayList<>();
        NbtList list = nbt.getList("path", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound p = list.getCompound(i);
            path.add(new BlockPos(p.getInt("x"), p.getInt("y"), p.getInt("z")));
        }
        return new RailLine(id, name, start, end, type, path, status);
    }
}
