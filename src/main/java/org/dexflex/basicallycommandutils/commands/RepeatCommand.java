package org.dexflex.basicallycommandutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class RepeatCommand {

    private static final Queue<ScheduledExecution> scheduledExecutions = new LinkedList<>();
    private static boolean tickHandlerRegistered = false;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        if (!tickHandlerRegistered) {
            registerTickHandler();
            tickHandlerRegistered = true;
        }

        dispatcher.register(CommandManager.literal("repeat")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                        .then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
                                .then(CommandManager.literal("run")
                                        .fork(dispatcher.getRoot(), ctx -> {
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            int delay = IntegerArgumentType.getInteger(ctx, "delay");

                                            java.util.Collection<ServerCommandSource> sources = new java.util.ArrayList<>();

                                            if (delay == 0) {
                                                for (int i = 0; i < amount; i++) {
                                                    sources.add(ctx.getSource());
                                                }
                                            } else {
                                                scheduledExecutions.add(new ScheduledExecution(
                                                        ctx.getSource(),
                                                        amount,
                                                        delay,
                                                        ctx.getInput().substring(ctx.getInput().indexOf("run") + 4)
                                                ));
                                                ctx.getSource().sendFeedback(() ->
                                                                Text.literal("Scheduled " + amount + " executions with " + delay + " tick delay"),
                                                        true
                                                );
                                            }

                                            return sources;
                                        })))));
    }

    private static void registerTickHandler() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            List<ScheduledExecution> toProcess = new ArrayList<>(scheduledExecutions);

            for (ScheduledExecution scheduled : toProcess) {
                if (!scheduledExecutions.contains(scheduled)) {
                    continue;
                }

                scheduled.ticksRemaining--;

                if (scheduled.ticksRemaining <= 0) {
                    try {
                        server.getCommandManager().executeWithPrefix(scheduled.source, scheduled.command);
                    } catch (Exception e) {
                        scheduled.source.sendError(Text.literal("Error: " + e.getMessage()));
                    }

                    scheduled.executionsRemaining--;

                    if (scheduled.executionsRemaining > 0) {
                        scheduled.ticksRemaining = scheduled.delayTicks;
                    } else {
                        scheduledExecutions.remove(scheduled);
                    }
                }
            }
        });
    }

    private static class ScheduledExecution {
        final ServerCommandSource source;
        final String command;
        int executionsRemaining;
        final int delayTicks;
        int ticksRemaining;

        ScheduledExecution(ServerCommandSource source, int amount, int delay, String command) {
            this.source = source;
            this.command = command;
            this.executionsRemaining = amount;
            this.delayTicks = delay;
            this.ticksRemaining = delay;
        }
    }
}