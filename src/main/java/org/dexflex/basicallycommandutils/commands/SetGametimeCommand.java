package org.dexflex.basicallycommandutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.level.LevelProperties;
import org.dexflex.basicallycommandutils.mixin.LevelPropertiesAccessor;

public class SetGametimeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("setgametime")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.argument("value", LongArgumentType.longArg(0))
                        .executes(ctx -> {

                            long value = LongArgumentType.getLong(ctx, "value");

                            ServerWorld world = ctx.getSource().getWorld();
                            WorldProperties props = world.getLevelProperties();

                            LevelProperties levelProps = (LevelProperties) props;

                            ((LevelPropertiesAccessor) levelProps).setGameTime(value);

                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("GameTime set to: " + value),
                                    true
                            );

                            return (int) value;
                        })
                )
        );
    }
}