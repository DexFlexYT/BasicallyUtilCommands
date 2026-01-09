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
                .then(CommandManager.argument("origin", Vec3ArgumentType.vec3())
                        .then(CommandManager.argument("target", Vec3ArgumentType.vec3())
                                .executes(ctx -> executeVecs(ctx, 1.0f))
                                .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                                        .executes(ctx -> executeVecs(ctx, FloatArgumentType.getFloat(ctx, "scale")))
                                )
                        )
                )
                .then(CommandManager.argument("originEntity", EntityArgumentType.entity())
                        .then(CommandManager.argument("targetEntity", EntityArgumentType.entity())
                                .executes(ctx -> executeEntities(ctx, 1.0f))
                                .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                                        .executes(ctx -> executeEntities(ctx, FloatArgumentType.getFloat(ctx, "scale")))
                                )
                        )
                )
                .then(CommandManager.argument("originEntity", EntityArgumentType.entity())
                        .then(CommandManager.argument("targetVec", Vec3ArgumentType.vec3())
                                .executes(ctx -> executeEntityVec(ctx, 1.0f))
                                .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                                        .executes(ctx -> executeEntityVec(ctx, FloatArgumentType.getFloat(ctx, "scale")))
                                )
                        )
                )
                .then(CommandManager.argument("originVec", Vec3ArgumentType.vec3())
                        .then(CommandManager.argument("targetEntity", EntityArgumentType.entity())
                                .executes(ctx -> executeVecEntity(ctx, 1.0f))
                                .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                                        .executes(ctx -> executeVecEntity(ctx, FloatArgumentType.getFloat(ctx, "scale")))
                                )
                        )
                )
        );
    }

    private static int executeVecs(CommandContext<ServerCommandSource> context, float scale) {
        Vec3d origin = Vec3ArgumentType.getVec3(context, "origin");
        Vec3d target = Vec3ArgumentType.getVec3(context, "target");
        return sendDistance(context, origin, target, scale);
    }

    private static int executeEntities(CommandContext<ServerCommandSource> context, float scale) throws CommandSyntaxException {
        Entity origin = EntityArgumentType.getEntity(context, "originEntity");
        Entity target = EntityArgumentType.getEntity(context, "targetEntity");
        return sendDistance(context, origin.getPos(), target.getPos(), scale);
    }

    private static int executeEntityVec(CommandContext<ServerCommandSource> context, float scale) throws CommandSyntaxException {
        Entity origin = EntityArgumentType.getEntity(context, "originEntity");
        Vec3d target = Vec3ArgumentType.getVec3(context, "targetVec");
        return sendDistance(context, origin.getPos(), target, scale);
    }

    private static int executeVecEntity(CommandContext<ServerCommandSource> context, float scale) throws CommandSyntaxException {
        Vec3d origin = Vec3ArgumentType.getVec3(context, "originVec");
        Entity target = EntityArgumentType.getEntity(context, "targetEntity");
        return sendDistance(context, origin, target.getPos(), scale);
    }

    private static int sendDistance(CommandContext<ServerCommandSource> context, Vec3d origin, Vec3d target, float scale) {
        double distance = origin.distanceTo(target) * scale;
        context.getSource().sendFeedback(() -> Text.literal(String.format("Distance: %.3f", distance)), false);
        return (int) distance;
    }
}
