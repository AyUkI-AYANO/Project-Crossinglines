package com.crossinglines.client;

import com.crossinglines.model.RailType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.entity.player.PlayerEntity;

public class RoutePlannerScreen extends Screen {
    private final PlayerEntity player;
    private BlockPos start;
    private BlockPos end;
    private RailType type = RailType.SURFACE;
    private static final RailType[] TYPE_CYCLE = {RailType.SURFACE, RailType.EMBANKMENT, RailType.UNDERGROUND};

    protected RoutePlannerScreen(PlayerEntity player) {
        super(Text.literal("CrossingLines Planner"));
        this.player = player;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 50;

        addDrawableChild(ButtonWidget.builder(Text.literal("Set Start"), b -> start = player.getBlockPos())
                .dimensions(cx - 100, y, 95, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Set End"), b -> end = player.getBlockPos())
                .dimensions(cx + 5, y, 95, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Rail Type: " + type.name()), b -> {
                    type = nextType(type);
                    b.setMessage(Text.literal("Rail Type: " + type.name()));
                }).dimensions(cx - 100, y + 26, 200, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Plan Route"), b -> sendPlanCommand())
                .dimensions(cx - 100, y + 52, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Start Build"), b -> sendBuildCommand())
                .dimensions(cx - 100, y + 78, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(cx - 100, y + 104, 200, 20).build());
    }

    private void sendPlanCommand() {
        if (start == null || end == null) return;
        String cmd = String.format("cl plan %d %d %d %d %d %d %s",
                start.getX(), start.getY(), start.getZ(),
                end.getX(), end.getY(), end.getZ(),
                type.name().toLowerCase());
        executeClientCommand(cmd);
    }

    private void sendBuildCommand() {
        executeClientCommand("cl build_latest");
    }

    private void executeClientCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatCommand(command);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int cx = width / 2;
        int baseY = height / 2 - 80;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("CrossingLines b1.2 Planner"), cx, baseY, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Start: " + toShort(start)), cx, baseY + 14, 0xAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("End: " + toShort(end)), cx, baseY + 28, 0xAAAAAA);
    }

    private RailType nextType(RailType current) {
        for (int i = 0; i < TYPE_CYCLE.length; i++) {
            if (TYPE_CYCLE[i] == current) {
                return TYPE_CYCLE[(i + 1) % TYPE_CYCLE.length];
            }
        }
        return RailType.SURFACE;
    }

    private String toShort(BlockPos pos) {
        if (pos == null) return "Not set";
        Vec3i v = pos;
        return v.getX() + "," + v.getY() + "," + v.getZ();
    }
}
