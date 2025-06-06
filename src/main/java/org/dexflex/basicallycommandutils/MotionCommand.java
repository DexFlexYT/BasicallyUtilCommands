package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;

public class MotionCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("motion")
                .requires(source -> source.hasPermissionLevel(2))

                // /motion set <targets> <strength>
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                .then(CommandManager.argument("strength", FloatArgumentType.floatArg())
                                        .executes(ctx -> execute(ctx, false))
                                ))
                )

                // /motion add <targets> <strength>
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                .then(CommandManager.argument("strength", FloatArgumentType.floatArg())
                                        .executes(ctx -> execute(ctx, true))
                                ))
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context, boolean additive) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgumentType.getEntities(context, "targets");
        float strength = FloatArgumentType.getFloat(context, "strength");

        Vec2f rot = context.getSource().getRotation();
        float yawRad = (float) Math.toRadians(-rot.y);
        float pitchRad = (float) Math.toRadians(-rot.x);

        double x = Math.cos(pitchRad) * Math.sin(yawRad);
        double y = Math.sin(pitchRad);
        double z = Math.cos(pitchRad) * Math.cos(yawRad);

        Vec3d direction = new Vec3d(x, y, z).normalize().multiply(strength);

        for (Entity entity : entities) {
            if (additive) {
                entity.addVelocity(direction.x, direction.y, direction.z);
            } else {
                Vec3d currentVel = entity.getVelocity();
                Vec3d adjustment = direction.subtract(currentVel);
                entity.addVelocity(adjustment.x, adjustment.y, adjustment.z);
            }
            entity.velocityModified = true;
        }

        context.getSource().sendFeedback(
                () -> Text.literal((additive ? "Added" : "Set") + " motion for " + entities.size() + " entities with strength " + strength),
                true
        );

        return entities.size();
    }
}
