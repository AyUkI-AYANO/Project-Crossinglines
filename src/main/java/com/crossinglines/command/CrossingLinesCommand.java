package com.crossinglines.command;

import com.crossinglines.build.BuildTaskQueue;
import com.crossinglines.model.RailLine;
import com.crossinglines.model.RailSettings;
import com.crossinglines.model.RailType;
import com.crossinglines.planner.RailPlannerService;
import com.crossinglines.state.RailLineState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.command.argument.BlockPosArgumentType.blockPos;
import static net.minecraft.command.argument.BlockPosArgumentType.getBlockPos;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class CrossingLinesCommand {
    private static final RailPlannerService PLANNER = new RailPlannerService();

    private CrossingLinesCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register(CrossingLinesCommand::registerRoot);
    }

    private static void registerRoot(CommandDispatcher<ServerCommandSource> dispatcher,
                                     CommandRegistryAccess access,
                                     CommandManager.RegistrationEnvironment env) {
        var planCommand = literal("plan")
                .then(argument("start", blockPos())
                        .then(argument("end", blockPos())
                                .then(argument("type", word())
                                        .executes(ctx -> planLine(ctx.getSource(), getBlockPos(ctx, "start"), getBlockPos(ctx, "end"), getString(ctx, "type"), true))
                                        .then(argument("stations", word())
                                                .executes(ctx -> planLine(
                                                        ctx.getSource(),
                                                        getBlockPos(ctx, "start"),
                                                        getBlockPos(ctx, "end"),
                                                        getString(ctx, "type"),
                                                        parseBoolean(getString(ctx, "stations"))
                                                ))))));

        var buildLatestCommand = literal("build_latest")
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
                                ctx.getSource().sendFeedback(() -> Text.literal("Build queued: " + line.name()), true);
                                return 1;
                            })
                            .orElseGet(() -> {
                                ctx.getSource().sendError(Text.literal("No planned route. Use /cl plan first."));
                                return 0;
                            });
                });

        var renameLine = literal("rename_line")
                .then(argument("lineId", word())
                        .then(argument("name", greedyString())
                                .executes(ctx -> {
                                    RailLineState state = RailLineState.get(ctx.getSource().getWorld());
                                    UUID lineId = parseUuid(ctx.getSource(), getString(ctx, "lineId"));
                                    String newName = getString(ctx, "name");
                                    return state.byId(lineId).map(line -> {
                                        line.rename(newName);
                                        state.dirty();
                                        ctx.getSource().sendFeedback(() -> Text.literal("Line renamed: " + line.overview()), true);
                                        return 1;
                                    }).orElseGet(() -> {
                                        ctx.getSource().sendError(Text.literal("Line not found: " + lineId));
                                        return 0;
                                    });
                                })));

        var addStation = literal("add_station")
                .then(argument("lineId", word())
                        .then(argument("pos", blockPos())
                                .then(argument("name", greedyString())
                                        .executes(ctx -> {
                                            RailLineState state = RailLineState.get(ctx.getSource().getWorld());
                                            UUID lineId = parseUuid(ctx.getSource(), getString(ctx, "lineId"));
                                            BlockPos pos = getBlockPos(ctx, "pos");
                                            String name = getString(ctx, "name");
                                            return state.byId(lineId).map(line -> {
                                                line.addStation(name, pos);
                                                state.dirty();
                                                ctx.getSource().sendFeedback(() -> Text.literal("Station added to " + line.name() + ": " + name), true);
                                                return 1;
                                            }).orElseGet(() -> {
                                                ctx.getSource().sendError(Text.literal("Line not found: " + lineId));
                                                return 0;
                                            });
                                        }))));

        var addStationLatest = literal("add_station_latest")
                .then(argument("pos", blockPos())
                        .then(argument("name", greedyString())
                                .executes(ctx -> {
                                    RailLineState state = RailLineState.get(ctx.getSource().getWorld());
                                    BlockPos pos = getBlockPos(ctx, "pos");
                                    String name = getString(ctx, "name");
                                    return state.latest().map(line -> {
                                        line.addStation(name, pos);
                                        state.dirty();
                                        ctx.getSource().sendFeedback(() -> Text.literal("Station added to latest line: " + name), true);
                                        return 1;
                                    }).orElseGet(() -> {
                                        ctx.getSource().sendError(Text.literal("No latest line found."));
                                        return 0;
                                    });
                                })));

        var renameStation = literal("rename_station")
                .then(argument("lineId", word())
                        .then(argument("index", integer(0))
                                .then(argument("name", greedyString())
                                        .executes(ctx -> {
                                            RailLineState state = RailLineState.get(ctx.getSource().getWorld());
                                            UUID lineId = parseUuid(ctx.getSource(), getString(ctx, "lineId"));
                                            int stationIndex = getInteger(ctx, "index");
                                            String newName = getString(ctx, "name");
                                            return state.byId(lineId).map(line -> {
                                                if (!line.renameStation(stationIndex, newName)) {
                                                    ctx.getSource().sendError(Text.literal("Invalid station index: " + stationIndex));
                                                    return 0;
                                                }
                                                state.dirty();
                                                ctx.getSource().sendFeedback(() -> Text.literal("Station renamed at index " + stationIndex), true);
                                                return 1;
                                            }).orElseGet(() -> {
                                                ctx.getSource().sendError(Text.literal("Line not found: " + lineId));
                                                return 0;
                                            });
                                        }))));

        var listLines = literal("list_lines")
                .executes(ctx -> {
                    RailLineState state = RailLineState.get(ctx.getSource().getWorld());
                    if (state.all().isEmpty()) {
                        ctx.getSource().sendError(Text.literal("No rail lines recorded yet."));
                        return 0;
                    }

                    ctx.getSource().sendFeedback(() -> Text.literal("=== CrossingLines Line Manager ==="), false);
                    int idx = 0;
                    for (RailLine line : state.all()) {
                        int lineIndex = idx++;
                        ctx.getSource().sendFeedback(() -> Text.literal((lineIndex + 1) + ". " + line.overview()), false);
                        for (int i = 0; i < line.stations().size(); i++) {
                            int stationIndex = i;
                            var station = line.stations().get(i);
                            ctx.getSource().sendFeedback(() -> Text.literal("   - [" + stationIndex + "] " + station.name() + " @ " + station.pos().toShortString()), false);
                        }
                    }
                    return 1;
                });

        var cancelBuildCommand = literal("cancel_build")
                .executes(ctx -> {
                    int cancelled = BuildTaskQueue.cancelAll();
                    if (cancelled <= 0) {
                        ctx.getSource().sendError(Text.literal("No build task in queue."));
                        return 0;
                    }
                    int total = cancelled;
                    ctx.getSource().sendFeedback(() -> Text.literal("Cancelled build tasks: " + total), true);
                    return 1;
                });

        var queueStatusCommand = literal("queue_status")
                .executes(ctx -> {
                    int pending = BuildTaskQueue.pendingTasks();
                    ctx.getSource().sendFeedback(() -> Text.literal("Build queue size: " + pending), false);
                    return 1;
                });

        dispatcher.register(literal("cl")
                .then(planCommand)
                .then(buildLatestCommand)
                .then(renameLine)
                .then(addStation)
                .then(addStationLatest)
                .then(renameStation)
                .then(listLines)
                .then(cancelBuildCommand)
                .then(queueStatusCommand));
    }

    private static int planLine(ServerCommandSource source, BlockPos start, BlockPos end, String rawType, boolean autoStations) {
        RailType type = parseType(rawType);

        RailSettings settings = RailSettings.defaults();
        RailLine line = PLANNER.plan(
                source.getWorld(),
                start,
                end,
                type,
                settings
        );

        if (line.path().isEmpty()) {
            source.sendError(Text.literal("No valid route found. Try different points or type."));
            return 0;
        }

        if (autoStations) {
            line.addStation("Start Station", start);
            line.addStation("Terminal Station", end);
        }

        RailLineState state = RailLineState.get(source.getWorld());
        state.put(line);

        source.sendFeedback(() -> Text.literal("Route planned: " + line.name() + ", nodes=" + line.path().size() + ", stations=" + line.stations().size()), true);
        return 1;
    }

    private static RailType parseType(String raw) {
        return switch (raw.toLowerCase()) {
            case "embankment", "e" -> RailType.EMBANKMENT;
            case "underground", "u" -> RailType.UNDERGROUND;
            default -> RailType.SURFACE;
        };
    }

    private static boolean parseBoolean(String raw) {
        return switch (raw.toLowerCase()) {
            case "off", "false", "0", "no" -> false;
            default -> true;
        };
    }

    private static UUID parseUuid(ServerCommandSource source, String raw) throws CommandSyntaxException {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            source.sendError(Text.literal("Invalid UUID: " + raw));
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create("Invalid UUID");
        }
    }
}
