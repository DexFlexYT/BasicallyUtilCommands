package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;

public class ForceCrawlCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("forcecrawl")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                        .executes(ctx -> execute(ctx.getSource(),
                                EntityArgumentType.getPlayers(ctx, "targets"))))
        );
    }

    private static int execute(ServerCommandSource source, Collection<? extends PlayerEntity> players)
            throws CommandSyntaxException {
        int count = 0;

        for (PlayerEntity player : players) {
            if (player.getPose() != EntityPose.SWIMMING) {
                player.setPose(EntityPose.SWIMMING);
                count++;
            }
        }

        source.sendFeedback(
                () -> Text.literal("Forced player(s) to crawl."),
                true
        );

        return count;
    }
}
