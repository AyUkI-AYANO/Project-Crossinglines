package com.crossinglines.command;

import com.crossinglines.build.BuildTaskQueue;
import com.crossinglines.model.RailLine;
import com.crossinglines.model.RailSettings;
import com.crossinglines.model.RailType;
import com.crossinglines.planner.RailPlannerService;
import com.crossinglines.state.RailLineState;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.command.argument.BlockPosArgumentType.blockPos;
import static net.minecraft.command.argument.BlockPosArgumentType.getBlockPos;
import static net.minecraft.command.argument.StringArgumentType.getString;
import static net.minecraft.command.argument.StringArgumentType.word;

public final class CrossingLinesCommand {
    private static final RailPlannerService PLANNER = new RailPlannerService();

    private CrossingLinesCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register(CrossingLinesCommand::registerRoot);
    }

    private static void registerRoot(CommandDispatcher<ServerCommandSource> dispatcher,
                                     CommandRegistryAccess access,
                                     CommandManager.RegistrationEnvironment env) {
        dispatcher.register(literal("cl")
                .then(literal("plan")
                        .then(argument("start", blockPos())
                                .then(argument("end", blockPos())
                                        .then(argument("type", word())
                                                .executes(ctx -> {
                                                    BlockPos start = getBlockPos(ctx, "start");
                                                    BlockPos end = getBlockPos(ctx, "end");
                                                    RailType type = parseType(getString(ctx, "type"));

                                                    RailSettings settings = RailSettings.defaults();
                                                    RailLine line = PLANNER.plan(
                                                            ctx.getSource().getWorld(),
                                                            start,
                                                            end,
                                                            type,
                                                            settings
                                                    );

                                                    if (line.path().isEmpty()) {
                                                        ctx.getSource().sendError(Text.literal("未找到可行路径，请调整起终点或类型。"));
                                                        return 0;
                                                    }

                                                    RailLineState state = RailLineState.get(ctx.getSource().getWorld());
                                                    state.put(line);

                                                    ctx.getSource().sendFeedback(() -> Text.literal("线路已规划: " + line.name() + "，节点数=" + line.path().size()), true);
                                                    return 1;
                                                }))))
                .then(literal("build_latest")
                        .executes(ctx -> {
                            RailLineState state = RailLineState.get(ctx.getSource().getWorld());
                            RailSettings settings = RailSettings.defaults();

                            return state.latest()
                                    .map(line -> {
                                        BuildTaskQueue.enqueue(
                                                ctx.getSource().getWorld(),
                                                line,
                                                PLANNER.policyOf(line.type()),
                                                settings
                                        );
                                        ctx.getSource().sendFeedback(() -> Text.literal("已加入施工队列: " + line.name()), true);
                                        return 1;
                                    })
                                    .orElseGet(() -> {
                                        ctx.getSource().sendError(Text.literal("没有可施工的线路，请先使用 /cl plan。"));
                                        return 0;
                                    });
                        }))
        );
    }

    private static RailType parseType(String raw) {
        return switch (raw.toLowerCase()) {
            case "underground", "u", "地下" -> RailType.UNDERGROUND;
            default -> RailType.SURFACE;
        };
    }
}
