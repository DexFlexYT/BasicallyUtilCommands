package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.CommandFunctionArgumentType;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.world.ServerWorld;
import java.util.Collection;
public class RaycastCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("raycast")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.argument("steps", IntegerArgumentType.integer(1))
                        .then(CommandManager.argument("step_length", FloatArgumentType.floatArg(0.01f))
                                .then(CommandManager.argument("function", CommandFunctionArgumentType.commandFunction())
                                        .executes(ctx -> {
                                                    Collection<CommandFunction<ServerCommandSource>> functions = CommandFunctionArgumentType.getFunctions(ctx, "function");
                                                    int steps = IntegerArgumentType.getInteger(ctx, "steps");
                                                    float stepLength = FloatArgumentType.getFloat(ctx, "step_length");
                                                    ServerCommandSource source = ctx.getSource();
                                                    ServerWorld world = source.getWorld();
                                                    EntityAnchorArgumentType.EntityAnchor anchor = source.getEntityAnchor();
                                                    Vec3d pos = anchor.positionAt(source);
                                                    Vec2f rot = source.getRotation();
                                                    Vec3d dir = Vec3d.fromPolar(rot.x, rot.y);
                                                    for (int i = 0; i < steps; i++) {
                                                        Vec3d stepPos = pos.add(dir.multiply(i * stepLength));
                                                        ServerCommandSource steppedSource = source.withPosition(stepPos);
                                                        for (CommandFunction<ServerCommandSource> function : functions) {
                                                            world.getServer().getCommandFunctionManager().execute(function, steppedSource.withSilent());
                                                        }
                                                    }
                                                    return 1;
                                                }
                                        )
                                )
                        )
                )
        );
    }
}
