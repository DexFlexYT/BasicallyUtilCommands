package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;

public class MotionCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("motion")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                        .then(CommandManager.argument("strength", FloatArgumentType.floatArg(0))
                                .executes(MotionCommand::execute)
                        )
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgumentType.getEntities(context, "targets");
        float strength = FloatArgumentType.getFloat(context, "strength");

        Vec2f rot = context.getSource().getRotation();
        float yaw = rot.y;
        float pitch = rot.x;

        float yawRad = (float) Math.toRadians(-yaw);
        float pitchRad = (float) Math.toRadians(-pitch);

        double x = Math.cos(pitchRad) * Math.sin(yawRad);
        double y = Math.sin(pitchRad);
        double z = Math.cos(pitchRad) * Math.cos(yawRad);

        Vec3d direction = new Vec3d(x, y, z).normalize().multiply(strength);

        for (Entity entity : entities) {
            entity.addVelocity(direction.x, direction.y, direction.z);
            entity.velocityModified = true;
        }

        context.getSource().sendFeedback(
                () -> Text.literal("Launched " + entities.size() + " entities with strength " + strength),
                true
        );

        return entities.size();
    }
}
