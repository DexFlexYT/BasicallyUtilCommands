package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

public class RaycastCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("raycast")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("steps", IntegerArgumentType.integer(1))
                        .then(CommandManager.argument("step_length", FloatArgumentType.floatArg(0.01f))
                                .then(CommandManager.literal("run")
                                        .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    int steps = IntegerArgumentType.getInteger(ctx, "steps");
                                                    float stepLength = FloatArgumentType.getFloat(ctx, "step_length");
                                                    String command = StringArgumentType.getString(ctx, "command");
                                                    ServerCommandSource source = ctx.getSource();

                                                    ServerWorld world = source.getWorld();
                                                    EntityAnchorArgumentType.EntityAnchor anchor = source.getEntityAnchor();
                                                    Vec3d startPos = anchor.positionAt(source);
                                                    Vec2f rot = source.getRotation();
                                                    Vec3d dir = Vec3d.fromPolar(rot.x, rot.y);

                                                    MarkerEntity marker = new MarkerEntity(EntityType.MARKER, world);
                                                    marker.setPosition(startPos);
                                                    marker.addCommandTag("BUC.raycast");
                                                    world.spawnEntity(marker);

                                                    runRaycastStep(world, source, command, startPos, dir, stepLength, steps, 0, marker.getUuid());

                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
        );
    }
    private static void runRaycastStep(ServerWorld world, ServerCommandSource originalSource, String command,
                                       Vec3d startPos, Vec3d dir, float stepLength, int maxSteps, int currentStep, UUID markerId) {
        if (currentStep >= maxSteps) return;

        MarkerEntity marker = (MarkerEntity) world.getEntity(markerId);
        if (marker == null || !marker.isAlive()) return;

        Vec3d stepPos = startPos.add(dir.multiply(currentStep * stepLength));
        ServerCommandSource stepSource = originalSource.withPosition(stepPos).withEntity(marker);
        world.getServer().getCommandManager().executeWithPrefix(stepSource.withSilent(), command);

        world.getServer().submit(() ->
                runRaycastStep(world, originalSource, command, startPos, dir, stepLength, maxSteps, currentStep + 1, markerId)
        );
    }
}
