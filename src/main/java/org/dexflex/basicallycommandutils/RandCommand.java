package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import com.mojang.brigadier.context.CommandContext;

import java.util.Random;

public class RandCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
        LiteralArgumentBuilder<ServerCommandSource> rand = CommandManager.literal("rand")
                .requires(source -> source.hasPermissionLevel(2));

        rand.then(CommandManager.literal("offset")
                .then(CommandManager.argument("range", StringArgumentType.string())
                        .then(CommandManager.argument("snapping", StringArgumentType.string())
                                .redirect(dispatcher.getRoot(), ctx -> {
                                    Vec3d offset = getRandomOffset(ctx);
                                    return ctx.getSource().withPosition(ctx.getSource().getPosition().add(offset));
                                }))));

        rand.then(CommandManager.literal("rotate")
                .then(CommandManager.argument("range", StringArgumentType.string())
                        .then(CommandManager.argument("snapping", StringArgumentType.string())
                                .redirect(dispatcher.getRoot(), ctx -> {
                                    Vec2f rotation = getRandomRotation(ctx);
                                    return ctx.getSource().withRotation(rotation);
                                }))));

        dispatcher.register(rand);
    }

    private static Vec3d getRandomOffset(CommandContext<ServerCommandSource> ctx) {
        String[] parts = StringArgumentType.getString(ctx, "range").split("-");
        double x = 0, y = 0, z = 0;
        if (parts.length == 1) {
            x = y = z = Double.parseDouble(parts[0]);
        } else if (parts.length == 2) {
            x = Double.parseDouble(parts[0]);
            y = Double.parseDouble(parts[1]);
            z = 0;
        } else if (parts.length == 3) {
            x = Double.parseDouble(parts[0]);
            y = Double.parseDouble(parts[1]);
            z = Double.parseDouble(parts[2]);
        }

        double snapping = Double.parseDouble(StringArgumentType.getString(ctx, "snapping"));
        Random rand = new Random();
        double dx = snap((rand.nextDouble() * 2 - 1) * x, snapping);
        double dy = snap((rand.nextDouble() * 2 - 1) * y, snapping);
        double dz = snap((rand.nextDouble() * 2 - 1) * z, snapping);

        return new Vec3d(dx, dy, dz);
    }

    private static Vec2f getRandomRotation(CommandContext<ServerCommandSource> ctx) {
        String[] parts = StringArgumentType.getString(ctx, "range").split("-");
        float yaw = 0, pitch = 0;
        if (parts.length == 1) {
            yaw = pitch = Float.parseFloat(parts[0]);
        } else if (parts.length == 2) {
            yaw = Float.parseFloat(parts[0]);
            pitch = Float.parseFloat(parts[1]);
        }

        float snapping = Float.parseFloat(StringArgumentType.getString(ctx, "snapping"));
        Random rand = new Random();
        float yawOffset = snap((float)(rand.nextDouble() * 2 - 1) * yaw, snapping);
        float pitchOffset = snap((float)(rand.nextDouble() * 2 - 1) * pitch, snapping);

        Vec2f current = ctx.getSource().getRotation();
        return new Vec2f(current.x + yawOffset, current.y + pitchOffset);
    }

    private static float snap(float val, float step) {
        if (step == 0) return val;
        return Math.round(val / step) * step;
    }

    private static double snap(double val, double step) {
        if (step == 0) return val;
        return Math.round(val / step) * step;
    }
}
