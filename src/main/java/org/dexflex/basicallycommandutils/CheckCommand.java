package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;

public class CheckCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("check")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.literal("blocks")
                        .then(CommandManager.argument("distance", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("block", BlockStateArgumentType.blockState(registryAccess))
                                        .executes(CheckCommand::executeBlocks)
                                ))
                )
                .then(CommandManager.literal("intersection")
                        .then(CommandManager.argument("selector", EntityArgumentType.entities())
                                .executes(CheckCommand::executeIntersection)
                        )
                )
        );
    }

    private static int executeBlocks(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerWorld world = source.getWorld();
        BlockPos origin = BlockPos.ofFloored(source.getPosition());
        int distance = IntegerArgumentType.getInteger(ctx, "distance");
        BlockStateArgument blockArg = BlockStateArgumentType.getBlockState(ctx, "block");

        int count = 0;
        for (int dx = -distance; dx <= distance; dx++) {
            for (int dy = -distance; dy <= distance; dy++) {
                for (int dz = -distance; dz <= distance; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (blockArg.test(new CachedBlockPosition(world, pos, true))) {
                        count++;
                    }
                }
            }
        }

        final int found = count;
        final int dist = distance;
        source.sendFeedback(
                () -> Text.literal("Found " + found + " matching blocks within " + dist + " blocks."),
                false
        );
        return found;
    }

    private static int executeIntersection(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        Vec3d pos = source.getPosition();
        double x = pos.x, y = pos.y, z = pos.z;

        Collection<? extends Entity> ents = EntityArgumentType.getEntities(ctx, "selector");

        int hits = 0;
        for (Entity e : ents) {
            Box box = e.getBoundingBox();
            if (box.contains(x, y, z)) {
                hits++;
            }
        }

        final int result = hits;
        source.sendFeedback(() ->
                        Text.literal(String.format("Found %d intersections.", result)),
                false
        );
        return result;
    }
}
