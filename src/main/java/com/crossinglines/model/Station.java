package com.crossinglines.model;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class Station {
    private final UUID id;
    private String name;
    private final BlockPos pos;

    public Station(UUID id, String name, BlockPos pos) {
        this.id = id;
        this.name = name;
        this.pos = pos;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public BlockPos pos() {
        return pos;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("id", id);
        nbt.putString("name", name);
        nbt.putInt("x", pos.getX());
        nbt.putInt("y", pos.getY());
        nbt.putInt("z", pos.getZ());
        return nbt;
    }

    public static Station fromNbt(NbtCompound nbt) {
        return new Station(
                nbt.getUuid("id"),
                nbt.getString("name"),
                new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"))
        );
    }
}
