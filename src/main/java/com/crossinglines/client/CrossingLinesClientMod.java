package com.crossinglines.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CrossingLinesClientMod implements ClientModInitializer {
    private static KeyBinding openPlannerKey;

    @Override
    public void onInitializeClient() {
        openPlannerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.crossinglines.open_planner",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.crossinglines.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPlannerKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new RoutePlannerScreen(client.player));
                }
            }
        });
    }
}
