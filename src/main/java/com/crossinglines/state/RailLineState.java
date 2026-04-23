package com.crossinglines.state;

import com.crossinglines.model.RailLine;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.*;

public class RailLineState extends PersistentState {
    private static final String KEY = "crossinglines_lines";
    private final Map<UUID, RailLine> lines = new LinkedHashMap<>();
    private UUID latestPlanned;

    public static RailLineState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(getType(), KEY);
    }

    public static Type<RailLineState> getType() {
        return new Type<>(RailLineState::new, RailLineState::fromNbt, null);
    }

    public void put(RailLine line) {
        lines.put(line.id(), line);
        latestPlanned = line.id();
        markDirty();
    }

    public Optional<RailLine> latest() {
        if (latestPlanned == null) return Optional.empty();
        return Optional.ofNullable(lines.get(latestPlanned));
    }

    public Collection<RailLine> all() {
        return lines.values();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        for (RailLine line : lines.values()) {
            list.add(line.toNbt());
        }
        nbt.put("lines", list);
        if (latestPlanned != null) {
            nbt.putUuid("latest", latestPlanned);
        }
        return nbt;
    }

    public static RailLineState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        RailLineState state = new RailLineState();
        NbtList list = nbt.getList("lines", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            RailLine line = RailLine.fromNbt(list.getCompound(i));
            state.lines.put(line.id(), line);
        }
        if (nbt.containsUuid("latest")) {
            state.latestPlanned = nbt.getUuid("latest");
        }
        return state;
    }
}
