package com.crossinglines.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class LineManagerScreen extends Screen {
    private final PlayerEntity player;
    private final Screen parent;
    private TextFieldWidget lineIdField;
    private TextFieldWidget lineNameField;
    private TextFieldWidget stationIndexField;
    private TextFieldWidget stationNameField;

    protected LineManagerScreen(PlayerEntity player, Screen parent) {
        super(Text.literal("CrossingLines 2.0.0 - Line Manager"));
        this.player = player;
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.passEvents = true;
        int cx = width / 2;
        int y = height / 2 - 90;

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh (/cl list_lines)"), b -> executeClientCommand("cl list_lines"))
                .dimensions(cx - 155, y, 310, 20).build());

        lineIdField = new TextFieldWidget(textRenderer, cx - 155, y + 28, 310, 18, Text.literal("Line ID"));
        lineIdField.setPlaceholder(Text.literal("线路 UUID"));
        addDrawableChild(lineIdField);

        lineNameField = new TextFieldWidget(textRenderer, cx - 155, y + 50, 310, 18, Text.literal("Line Name"));
        lineNameField.setPlaceholder(Text.literal("新线路名称"));
        addDrawableChild(lineNameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Rename Line"), b -> {
                    if (!lineIdField.getText().isBlank() && !lineNameField.getText().isBlank()) {
                        executeClientCommand("cl rename_line " + lineIdField.getText() + " " + lineNameField.getText());
                    }
                }).dimensions(cx - 155, y + 74, 150, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Add Station Here"), b -> {
                    if (!lineIdField.getText().isBlank()) {
                        var p = player.getBlockPos();
                        String stationName = stationNameField.getText().isBlank() ? "Station" : stationNameField.getText();
                        executeClientCommand("cl add_station " + lineIdField.getText() + " " + p.getX() + " " + p.getY() + " " + p.getZ() + " " + stationName);
                    }
                }).dimensions(cx + 5, y + 74, 150, 20)
                .build());

        stationIndexField = new TextFieldWidget(textRenderer, cx - 155, y + 100, 80, 18, Text.literal("Station Index"));
        stationIndexField.setPlaceholder(Text.literal("index"));
        stationIndexField.setText("0");
        addDrawableChild(stationIndexField);

        stationNameField = new TextFieldWidget(textRenderer, cx - 70, y + 100, 225, 18, Text.literal("Station Name"));
        stationNameField.setPlaceholder(Text.literal("站点名"));
        addDrawableChild(stationNameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Rename Station"), b -> {
                    if (!lineIdField.getText().isBlank() && !stationIndexField.getText().isBlank() && !stationNameField.getText().isBlank()) {
                        executeClientCommand("cl rename_station " + lineIdField.getText() + " " + stationIndexField.getText() + " " + stationNameField.getText());
                    }
                }).dimensions(cx - 155, y + 124, 310, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back to Planner"), b -> close())
                .dimensions(cx - 155, y + 148, 310, 20).build());
    }

    private void executeClientCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatCommand(command);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int cx = width / 2;
        int baseY = height / 2 - 116;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("CrossingLines 2.0.0 Line Manager"), cx, baseY, 0x8BE9FD);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("线路概览请通过 Refresh 后查看聊天栏输出。"), cx, baseY + 14, 0xCCCCCC);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("支持重命名线路、添加站点、重命名站点。"), cx, baseY + 26, 0xCCCCCC);
    }
}
