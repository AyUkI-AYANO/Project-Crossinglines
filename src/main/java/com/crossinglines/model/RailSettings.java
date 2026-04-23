package com.crossinglines.model;

public record RailSettings(
        int maxSlopePerStep,
        int lightSpacing,
        int buildBlocksPerTick,
        int undergroundDepth
) {
    public static RailSettings defaults() {
        return new RailSettings(1, 10, 64, 8);
    }
}
