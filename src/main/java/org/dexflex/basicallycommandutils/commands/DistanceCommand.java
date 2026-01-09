package org.dexflex.basicallycommandutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class DistanceCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("distance")
                .requires(source -> source.hasPermissionLevel(4))
                // vec → vec
                .then(CommandManager.argument("origin", Vec3ArgumentType.vec3())
                        .then(CommandManager.argument("target", Vec3ArgumentType.vec3())
                                .executes(ctx -> executeVecs(ctx, 1.0f))
                                .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                                        .executes(ctx ->
                                                executeVecs(ctx, FloatArgumentType.getFloat(ctx, "scale"))
                                        )
                                )
                        )
                )
                // entity → entity
                .then(CommandManager.argument("originEntity", EntityArgumentType.entity())
                        .then(CommandManager.argument("targetEntity", EntityArgumentType.entity())
                                .executes(ctx -> executeEntities(ctx, 1.0f))
                                .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                                        .executes(ctx ->
                                                executeEntities(ctx, FloatArgumentType.getFloat(ctx, "scale"))
                                        )
                                )
                        )
                )

                // entity → vec
                .then(CommandManager.argument("originEntity", EntityArgumentType.entity())
                        .then(CommandManager.argument("targetVec", Vec3ArgumentType.vec3())
                                .executes(ctx -> executeEntityVec(ctx, 1.0f))
                                .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                                        .executes(ctx ->
                                                executeEntityVec(ctx, FloatArgumentType.getFloat(ctx, "scale"))
                                        )
                                )
                        )
                )
                // vec → entity
                .then(CommandManager.argument("originVec", Vec3ArgumentType.vec3())
                        .then(CommandManager.argument("targetEntity", EntityArgumentType.entity())
                                .executes(ctx -> executeVecEntity(ctx, 1.0f))
                                .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                                        .executes(ctx ->
                                                executeVecEntity(ctx, FloatArgumentType.getFloat(ctx, "scale"))
                                        )
                                )
                        )
                )
        );
    }
    private static int executeVecs(CommandContext<ServerCommandSource> ctx, float scale) {
        Vec3d origin = Vec3ArgumentType.getVec3(ctx, "origin");
        Vec3d target = Vec3ArgumentType.getVec3(ctx, "target");
        return sendDistance(ctx, origin, target, scale);
    }

    private static int executeEntities(CommandContext<ServerCommandSource> ctx, float scale)
            throws CommandSyntaxException {
        Entity origin = EntityArgumentType.getEntity(ctx, "originEntity");
        Entity target = EntityArgumentType.getEntity(ctx, "targetEntity");
        return sendDistance(ctx, origin.getPos(), target.getPos(), scale);
    }
    private static int executeEntityVec(CommandContext<ServerCommandSource> ctx, float scale)
            throws CommandSyntaxException {

        Entity origin = EntityArgumentType.getEntity(ctx, "originEntity");
        Vec3d target = Vec3ArgumentType.getVec3(ctx, "targetVec");
        return sendDistance(ctx, origin.getPos(), target, scale);
    }
    private static int executeVecEntity(CommandContext<ServerCommandSource> ctx, float scale)
            throws CommandSyntaxException {

        Vec3d origin = Vec3ArgumentType.getVec3(ctx, "originVec");
        Entity target = EntityArgumentType.getEntity(ctx, "targetEntity");
        return sendDistance(ctx, origin, target.getPos(), scale);
    }
    private static int sendDistance(CommandContext<ServerCommandSource> ctx,
                                    Vec3d origin,
                                    Vec3d target,
                                    float scale) {
        final double result = origin.distanceTo(target) * scale;

        ctx.getSource().sendFeedback(
                () -> Text.literal(String.format("Distance: %.3f", result)),
                false
        );
        return (int) result;
    }
}