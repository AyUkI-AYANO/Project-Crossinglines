package com.crossinglines.client;

import com.crossinglines.model.RailType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

public class RoutePlannerScreen extends Screen {
    private final PlayerEntity player;
    private static BlockPos lastStart;
    private static BlockPos lastEnd;
    private static RailType lastType = RailType.SURFACE;
    private static boolean lastEnableStations = true;
    private static final List<BlockPos> lastExtraStations = new ArrayList<>();

    private BlockPos start;
    private BlockPos end;
    private RailType type = RailType.SURFACE;
    private boolean enableStations = true;
    private final List<BlockPos> extraStations = new ArrayList<>();
    private static final RailType[] TYPE_CYCLE = {RailType.SURFACE, RailType.EMBANKMENT, RailType.UNDERGROUND};

    protected RoutePlannerScreen(PlayerEntity player) {
        super(Text.literal("CrossingLines Planner"));
        this.player = player;
    }

    @Override
    protected void init() {
        restoreLastState();

        int cx = width / 2;
        int y = height / 2 - 90;

        addDrawableChild(ButtonWidget.builder(Text.literal("Set Start (Player)"), b -> start = player.getBlockPos())
                .dimensions(cx - 155, y, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Set End (Player)"), b -> end = player.getBlockPos())
                .dimensions(cx + 5, y, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Rail Type: " + type.name()), b -> {
                    type = nextType(type);
                    b.setMessage(Text.literal("Rail Type: " + type.name()));
                }).dimensions(cx - 155, y + 24, 310, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(stationToggleText(), b -> {
                    enableStations = !enableStations;
                    b.setMessage(stationToggleText());
                }).dimensions(cx - 155, y + 48, 310, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Add Extra Station (Player Pos)"), b -> extraStations.add(player.getBlockPos()))
                .dimensions(cx - 155, y + 72, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Remove Last Station"), b -> {
                    if (!extraStations.isEmpty()) {
                        extraStations.remove(extraStations.size() - 1);
                    }
                }).dimensions(cx + 5, y + 72, 150, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Plan Route"), b -> sendPlanCommand())
                .dimensions(cx - 155, y + 96, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Start Build"), b -> sendBuildCommand())
                .dimensions(cx + 5, y + 96, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Open Line Manager"), b -> client.setScreen(new LineManagerScreen(player, this)))
                .dimensions(cx - 155, y + 120, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("List Lines (Chat)"), b -> executeClientCommand("cl list_lines"))
                .dimensions(cx + 5, y + 120, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(cx - 155, y + 144, 310, 20).build());
    }

    private Text stationToggleText() {
        return Text.literal("Stations: " + (enableStations ? "ON" : "OFF") + " (起终点自动站)");
    }

    private void sendPlanCommand() {
        if (start == null || end == null) return;
        String cmd = String.format("cl plan %d %d %d %d %d %d %s %s",
                start.getX(), start.getY(), start.getZ(),
                end.getX(), end.getY(), end.getZ(),
                type.name().toLowerCase(),
                enableStations ? "on" : "off");
        executeClientCommand(cmd);

        if (enableStations) {
            int idx = 1;
            for (BlockPos stationPos : extraStations) {
                String stationName = "Station-" + idx++;
                executeClientCommand(String.format("cl add_station_latest %d %d %d %s",
                        stationPos.getX(), stationPos.getY(), stationPos.getZ(), stationName));
            }
        }

        saveLastState();
    }

    private void sendBuildCommand() {
        executeClientCommand("cl build_latest");
        saveLastState();
    }

    private void executeClientCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatCommand(command);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void restoreLastState() {
        start = lastStart;
        end = lastEnd;
        type = lastType;
        enableStations = lastEnableStations;
        extraStations.clear();
        extraStations.addAll(lastExtraStations);
    }

    private void saveLastState() {
        lastStart = start;
        lastEnd = end;
        lastType = type;
        lastEnableStations = enableStations;
        lastExtraStations.clear();
        lastExtraStations.addAll(extraStations);
    }

    @Override
    public void close() {
        saveLastState();
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int cx = width / 2;
        int baseY = height / 2 - 118;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("CrossingLines 2.0.0 Planner"), cx, baseY, 0x8BE9FD);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("菜单打开时可继续移动；保留上次起终点与站点设置。"), cx, baseY + 14, 0xCCCCCC);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Start: " + toShort(start)), cx, baseY + 28, 0xAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("End: " + toShort(end)), cx, baseY + 42, 0xAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Extra Stations: " + extraStations.size()), cx, baseY + 56, 0xAAAAAA);
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
