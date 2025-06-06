package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class DistanceCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("distance")
                .requires(source -> source.hasPermissionLevel(0))
                .then(CommandManager.argument("target", Vec3ArgumentType.vec3())
                        .executes(context -> execute(context, 1.0f))
                        .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                                .executes(context -> execute(context, FloatArgumentType.getFloat(context, "scale")))
                        )
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context, float scale) {
        Vec3d from = context.getSource().getPosition();
        Vec3d to = Vec3ArgumentType.getVec3(context, "target");
        double distance = from.distanceTo(to) * scale;

        context.getSource().sendFeedback(() -> Text.literal(String.format("Distance: %.3f", distance)), false);
        return (int) distance;
    }
}
