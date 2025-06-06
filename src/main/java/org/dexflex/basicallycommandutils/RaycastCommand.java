package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RaycastCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("raycast")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(argument("steps", IntegerArgumentType.integer(1))
                                .then(argument("step_length", DoubleArgumentType.doubleArg(0.01))
                                        .then(argument("command", StringArgumentType.greedyString())
                                                .executes(ctx -> executeRaycast(ctx, dispatcher))
                                        )))
        );
    }
    private static int executeRaycast(CommandContext<ServerCommandSource> ctx,
                                      CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCommandSource original = ctx.getSource().withSilent();
        int steps = IntegerArgumentType.getInteger(ctx, "steps");
        double stepLength = DoubleArgumentType.getDouble(ctx, "step_length");
        String commandString = StringArgumentType.getString(ctx, "command");
        ServerWorld world = original.getWorld();
        MinecraftServer server = world.getServer();

        Vec2f rot = original.getRotation();
        Vec3d direction = Vec3d.fromPolar(rot.x, rot.y);
        Vec3d startPos = original.getPosition();

        MarkerEntity marker = new MarkerEntity(EntityType.MARKER, world);
        marker.setPosition(startPos);
        marker.setYaw(rot.x);
        marker.setPitch(rot.y);
        world.spawnEntity(marker);

        ParseResults<ServerCommandSource> parseResults = dispatcher.parse(commandString, original);
        if (!parseResults.getExceptions().isEmpty()) {
            ctx.getSource().sendError(net.minecraft.text.Text.literal(
                    parseResults.getExceptions().values().iterator().next().getMessage()
            ));
            marker.discard(); // cleanup
            return 0;
        }

        for (int i = 0; i < steps; i++) {
            Vec3d stepPos = startPos.add(direction.multiply(i * stepLength));
            marker.setPosition(stepPos);

            ServerCommandSource stepSource = original
                    .withEntity(marker)
                    .withPosition(stepPos)
                    .withRotation(marker.getRotationClient())
                    .withSilent();

            ParseResults<ServerCommandSource> stepParsed = dispatcher.parse(commandString, stepSource);

            server.getCommandManager().execute(stepParsed, commandString);
            if (!marker.isAlive()) {
                return i + 1;
            }
        }

        marker.discard(); // cleanup after all steps
        return steps;
    }

}
