package com.crossinglines;

import com.crossinglines.build.BuildTaskQueue;
import com.crossinglines.command.CrossingLinesCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrossingLinesMod implements ModInitializer {
    public static final String MOD_ID = "crossinglines";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CrossingLinesCommand.register();
        ServerTickEvents.END_SERVER_TICK.register(BuildTaskQueue::tickServer);
        LOGGER.info("CrossingLines b1.2 initialized");
    }
}
