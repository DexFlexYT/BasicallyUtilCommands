package org.dexflex.basicallycommandutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;

public class IgniteCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ignite")
                .requires(src -> src.hasPermissionLevel(2))

                // /ignite set <duration> <selector>
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("duration", IntegerArgumentType.integer(0))
                                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                        .executes(ctx -> execute(ctx, false))
                                ))
                )

                // /ignite add <duration> <selector>
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("duration", IntegerArgumentType.integer(0))
                                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                        .executes(ctx -> execute(ctx, true))
                                ))
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> ctx, boolean add) {
        ServerCommandSource source = ctx.getSource();
        int duration = IntegerArgumentType.getInteger(ctx, "duration");

        Collection<? extends Entity> targets;
        try {
            targets = EntityArgumentType.getEntities(ctx, "targets");
        } catch (CommandSyntaxException e) {
            source.sendError(Text.literal("Invalid selector: " + e.getMessage()));
            return 0;
        }

        int count = 0;
        for (Entity e : targets) {
            int newTicks = add
                    ? e.getFireTicks() + duration
                    : duration;
            e.setFireTicks(newTicks);
            count++;
        }

        final int affected = count;
        final int dur = duration;
        source.sendFeedback(() ->
                        Text.literal((add ? "Added " : "Set ") + dur + " ticks of fire to " + affected + " entities."),
                true
        );
        return affected;
    }
}
