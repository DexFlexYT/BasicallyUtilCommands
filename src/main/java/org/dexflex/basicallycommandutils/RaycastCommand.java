package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.CommandFunctionArgumentType;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class RaycastCommand {
    private static final Set<UUID> stoppedRaycasts = Collections.synchronizedSet(new HashSet<>());

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("raycast")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.argument("steps", IntegerArgumentType.integer(1))
                        .then(CommandManager.argument("step_length", FloatArgumentType.floatArg(0.01f))
                                .then(CommandManager.argument("function", CommandFunctionArgumentType.commandFunction())
                                        .executes(ctx -> {
                                            ServerCommandSource source = ctx.getSource();
                                            UUID sourceId = source.getEntity() != null ? source.getEntity().getUuid() : UUID.randomUUID();
                                            stoppedRaycasts.remove(sourceId);

                                            Collection<CommandFunction<ServerCommandSource>> functions = CommandFunctionArgumentType.getFunctions(ctx, "function");
                                            int steps = IntegerArgumentType.getInteger(ctx, "steps");
                                            float stepLength = FloatArgumentType.getFloat(ctx, "step_length");
                                            ServerWorld world = source.getWorld();
                                            EntityAnchorArgumentType.EntityAnchor anchor = source.getEntityAnchor();
                                            Vec3d pos = anchor.positionAt(source);
                                            Vec2f rot = source.getRotation();
                                            Vec3d dir = Vec3d.fromPolar(rot.x, rot.y);

                                            for (int i = 0; i < steps; i++) {
                                                if (stoppedRaycasts.contains(sourceId)) break;
                                                Vec3d stepPos = pos.add(dir.multiply(i * stepLength));
                                                ServerCommandSource steppedSource = source.withPosition(stepPos);
                                                for (CommandFunction<ServerCommandSource> function : functions) {
                                                    world.getServer().getCommandFunctionManager().execute(function, steppedSource.withSilent());
                                                }
                                            }

                                            return 1;
                                        })
                                )
                        )
                )
        );

        dispatcher.register(CommandManager.literal("raycaststop")
                .requires(source -> source.hasPermissionLevel(4))
                .executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    if (source.getEntity() != null) {
                        stoppedRaycasts.add(source.getEntity().getUuid());
                    }
                    return 1;
                }));
    }
}
