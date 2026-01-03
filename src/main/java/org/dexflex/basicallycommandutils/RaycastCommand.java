package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

public class RaycastCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("raycast")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("steps", IntegerArgumentType.integer(1, 1024))
                        .then(CommandManager.argument("step_length", FloatArgumentType.floatArg(0.0f, 128f))
                                .then(CommandManager.literal("run")
                                        .fork(dispatcher.getRoot(), ctx -> {
                                            int steps = IntegerArgumentType.getInteger(ctx, "steps");
                                            float stepLength = FloatArgumentType.getFloat(ctx, "step_length");
                                            String command = ctx.getInput().substring(ctx.getInput().indexOf("run") + 4);
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

                                            source.sendFeedback(() ->
                                                            Text.literal("Raycasting " + steps + " steps with length " + stepLength),
                                                    false
                                            );

                                            return java.util.Collections.emptyList();
                                        })))));
    }

    private static void runRaycastStep(ServerWorld world, ServerCommandSource originalSource, String command,
                                       Vec3d startPos, Vec3d dir, float stepLength, int maxSteps, int currentStep, UUID markerId) {
        if (currentStep >= maxSteps) {
            MarkerEntity marker = (MarkerEntity) world.getEntity(markerId);
            if (marker != null) marker.discard();
            return;
        }

        MarkerEntity marker = (MarkerEntity) world.getEntity(markerId);
        if (marker == null || !marker.isAlive()) return;

        Vec3d stepPos = startPos.add(dir.multiply(currentStep * stepLength));
        marker.setPosition(stepPos);

        ServerCommandSource stepSource = originalSource
                .withPosition(stepPos)
                .withEntity(marker)
                .withSilent();

        try {
            world.getServer().getCommandManager().executeWithPrefix(stepSource, command);
        } catch (Exception e) {
            originalSource.sendError(Text.literal("Raycast error at step " + currentStep + ": " + e.getMessage()));
            marker.discard();
            return;
        }

        if (!marker.isAlive()) return;

        world.getServer().submit(() ->
                runRaycastStep(world, originalSource, command, startPos, dir, stepLength, maxSteps, currentStep + 1, markerId)
        );
    }
}