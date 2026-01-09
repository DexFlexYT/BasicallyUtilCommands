package org.dexflex.basicallycommandutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MotionCommand {

    private static final Map<Integer, Vec3d> LAST_POS = new HashMap<>();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("motion")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                .then(CommandManager.argument("strength", FloatArgumentType.floatArg())
                                        .executes(ctx -> execute(ctx, false, null, null, null, null))

                                        .then(CommandManager.literal("with")
                                                .then(CommandManager.argument("vx", DoubleArgumentType.doubleArg())
                                                        .then(CommandManager.argument("vy", DoubleArgumentType.doubleArg())
                                                                .then(CommandManager.argument("vz", DoubleArgumentType.doubleArg())
                                                                        .executes(ctx -> execute(
                                                                                ctx,
                                                                                false,
                                                                                new Vec3d(
                                                                                        DoubleArgumentType.getDouble(ctx, "vx"),
                                                                                        DoubleArgumentType.getDouble(ctx, "vy"),
                                                                                        DoubleArgumentType.getDouble(ctx, "vz")
                                                                                ),
                                                                                null,
                                                                                null,
                                                                                null
                                                                        ))
                                                                )
                                                        )
                                                )
                                        )

                                        .then(CommandManager.literal("to")
                                                .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                                        .executes(ctx -> execute(
                                                                ctx,
                                                                false,
                                                                null,
                                                                Vec3ArgumentType.getVec3(ctx, "pos"),
                                                                null,
                                                                null
                                                        ))
                                                )
                                                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                                                        .executes(ctx -> execute(
                                                                ctx,
                                                                false,
                                                                null,
                                                                null,
                                                                EntityArgumentType.getEntity(ctx, "entity"),
                                                                null
                                                        ))
                                                )
                                        )

                                        .then(CommandManager.literal("at")
                                                .then(CommandManager.argument("rotation", RotationArgumentType.rotation())
                                                        .executes(ctx -> {
                                                            Vec2f raw = RotationArgumentType
                                                                    .getRotation(ctx, "rotation")
                                                                    .toAbsoluteRotation(ctx.getSource());
                                                            return execute(ctx, false, null, null, null, new Vec2f(raw.x, raw.y));
                                                        })
                                                )
                                        )
                                )
                        )
                )

                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                .then(CommandManager.argument("strength", FloatArgumentType.floatArg())
                                        .executes(ctx -> execute(ctx, true, null, null, null, null))

                                        .then(CommandManager.literal("with")
                                                .then(CommandManager.argument("vx", DoubleArgumentType.doubleArg())
                                                        .then(CommandManager.argument("vy", DoubleArgumentType.doubleArg())
                                                                .then(CommandManager.argument("vz", DoubleArgumentType.doubleArg())
                                                                        .executes(ctx -> execute(
                                                                                ctx,
                                                                                true,
                                                                                new Vec3d(
                                                                                        DoubleArgumentType.getDouble(ctx, "vx"),
                                                                                        DoubleArgumentType.getDouble(ctx, "vy"),
                                                                                        DoubleArgumentType.getDouble(ctx, "vz")
                                                                                ),
                                                                                null,
                                                                                null,
                                                                                null
                                                                        ))
                                                                )
                                                        )
                                                )
                                        )

                                        .then(CommandManager.literal("to")
                                                .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                                        .executes(ctx -> execute(
                                                                ctx,
                                                                true,
                                                                null,
                                                                Vec3ArgumentType.getVec3(ctx, "pos"),
                                                                null,
                                                                null
                                                        ))
                                                )
                                                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                                                        .executes(ctx -> execute(
                                                                ctx,
                                                                true,
                                                                null,
                                                                null,
                                                                EntityArgumentType.getEntity(ctx, "entity"),
                                                                null
                                                        ))
                                                )
                                        )

                                        .then(CommandManager.literal("at")
                                                .then(CommandManager.argument("rotation", RotationArgumentType.rotation())
                                                        .executes(ctx -> {
                                                            Vec2f raw = RotationArgumentType
                                                                    .getRotation(ctx, "rotation")
                                                                    .toAbsoluteRotation(ctx.getSource());
                                                            return execute(ctx, true, null, null, null, new Vec2f(raw.x, raw.y));
                                                        })
                                                )
                                        )
                                )
                        )
                )

                .then(CommandManager.literal("multiply")
                        .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                .then(CommandManager.argument("factor", DoubleArgumentType.doubleArg())
                                        .executes(ctx -> executeMultiply(ctx, "xyz"))
                                        .then(CommandManager.argument("axes", StringArgumentType.word())
                                                .executes(ctx -> executeMultiply(
                                                        ctx,
                                                        StringArgumentType.getString(ctx, "axes")
                                                ))
                                        )
                                )
                        )
                )
        );
    }


    private static int executeMultiply(CommandContext<ServerCommandSource> ctx, String axes)
            throws CommandSyntaxException {

        Collection<? extends Entity> entities = EntityArgumentType.getEntities(ctx, "targets");
        double factor = DoubleArgumentType.getDouble(ctx, "factor");

        boolean ax = axes.contains("x");
        boolean ay = axes.contains("y");
        boolean az = axes.contains("z");

        int applied = 0;

        for (Entity e : entities) {
            int id = e.getId();
            Vec3d currentPos = e.getPos();
            Vec3d lastPos = LAST_POS.get(id);

            if (lastPos == null) {
                LAST_POS.put(id, currentPos);
                continue;
            }

            Vec3d trueVel = currentPos.subtract(lastPos);

            Vec3d newVel = new Vec3d(
                    ax ? trueVel.x * factor : trueVel.x,
                    ay ? trueVel.y * factor : trueVel.y,
                    az ? trueVel.z * factor : trueVel.z
            );

            Vec3d diff = newVel.subtract(trueVel);

            if (diff.lengthSquared() != 0.0) {
                e.addVelocity(diff.x, diff.y, diff.z);
                e.velocityModified = true;
                applied++;
            }

            LAST_POS.put(id, currentPos);
        }

        final int resultCount = applied;

        ctx.getSource().sendFeedback(
                () -> Text.literal("Multiplied motion for " + resultCount + " entities"),
                true
        );


        return applied;
    }

    private static int execute(
            CommandContext<ServerCommandSource> ctx,
            boolean additive,
            Vec3d explicitVec,
            Vec3d targetPos,
            Entity targetEntity,
            Vec2f explicitRot
    ) throws CommandSyntaxException {

        Collection<? extends Entity> entities = EntityArgumentType.getEntities(ctx, "targets");
        float strength = FloatArgumentType.getFloat(ctx, "strength");

        Vec3d uniformDir = null;

        if (explicitVec != null) {
            uniformDir = explicitVec.normalize().multiply(strength);
        } else if (explicitRot != null) {
            uniformDir = rotationToDirection(explicitRot).multiply(strength);
        } else if (targetPos == null && targetEntity == null) {
            uniformDir = rotationToDirection(ctx.getSource().getRotation()).multiply(strength);
        }

        int applied = 0;

        for (Entity e : entities) {
            Vec3d dir = uniformDir;

            if (targetPos != null) {
                Vec3d delta = targetPos.subtract(e.getPos());
                if (delta.lengthSquared() == 0.0) continue;
                dir = delta.normalize().multiply(strength);
            } else if (targetEntity != null) {
                Vec3d delta = targetEntity.getPos().subtract(e.getPos());
                if (delta.lengthSquared() == 0.0) continue;
                dir = delta.normalize().multiply(strength);
            }

            if (dir == null) continue;

            if (additive) {
                e.addVelocity(dir.x, dir.y, dir.z);
            } else {
                Vec3d cur = e.getVelocity();
                Vec3d adj = dir.subtract(cur);
                e.addVelocity(adj.x, adj.y, adj.z);
            }

            e.velocityModified = true;
            applied++;
        }

        final int resultCount = applied;

        ctx.getSource().sendFeedback(
                () -> Text.literal("Multiplied motion for " + resultCount + " entities"),
                true
        );


        return applied;
    }

    private static Vec3d rotationToDirection(Vec2f rot) {
        float yawRad = (float) Math.toRadians(-rot.y);
        float pitchRad = (float) Math.toRadians(-rot.x);

        double x = Math.cos(pitchRad) * Math.sin(yawRad);
        double y = Math.sin(pitchRad);
        double z = Math.cos(pitchRad) * Math.cos(yawRad);

        return new Vec3d(x, y, z).normalize();
    }
}
