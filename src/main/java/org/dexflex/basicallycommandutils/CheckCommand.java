package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.*;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.Collection;
import java.util.function.Predicate;

public class CheckCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("check")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.literal("blocks")
                        .then(CommandManager.argument("distance", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("block", BlockStateArgumentType.blockState(registryAccess))
                                        .executes(CheckCommand::executeBlocks)
                                )
                        )
                )
                .then(CommandManager.literal("intersection")
                        .then(CommandManager.argument("selector", EntityArgumentType.entities())
                                .executes(CheckCommand::executeIntersection)
                        )
                )

                .then(CommandManager.literal("collision")
                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                .executes(ctx -> executeCollision(ctx, null))
                                .then(CommandManager.argument("block_filter", BlockPredicateArgumentType.blockPredicate(registryAccess))
                                        .executes(ctx -> {
                                            var filter = BlockPredicateArgumentType.getBlockPredicate(ctx, "block_filter");
                                            return executeCollision(ctx, filter);
                                        })
                                )
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

    private static int executeCollision(CommandContext<ServerCommandSource> ctx,
                                        Predicate<CachedBlockPosition> filter)
            throws CommandSyntaxException {

        ServerCommandSource source = ctx.getSource();
        ServerWorld world = source.getWorld();
        Entity target = EntityArgumentType.getEntity(ctx, "target");
        Box entityBox = target.getBoundingBox();

        // Search area (expand slightly beyond entity box)
        int minX = MathHelper.floor(entityBox.minX - 1);
        int minY = MathHelper.floor(entityBox.minY - 1);
        int minZ = MathHelper.floor(entityBox.minZ - 1);
        int maxX = MathHelper.floor(entityBox.maxX + 1);
        int maxY = MathHelper.floor(entityBox.maxY + 1);
        int maxZ = MathHelper.floor(entityBox.maxZ + 1);

        int hits = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    if (state.isAir()) continue;

                    if (filter != null && !filter.test(new CachedBlockPosition(world, pos, true))) continue;

                    VoxelShape shape = state.getCollisionShape(world, pos);
                    if (shape.isEmpty()) continue;

                    if (VoxelShapes.matchesAnywhere(
                            shape.offset(pos.getX(), pos.getY(), pos.getZ()),
                            VoxelShapes.cuboid(entityBox),
                            (a, b) -> a && b)) {
                        hits++;
                    }
                }
            }
        }

        final int result = hits;
        source.sendFeedback(
                () -> Text.literal(result > 0
                        ? "Found " + result + " block collisions."
                        : "Not found any block collisions."),
                false
        );

        return result;
    }
}
