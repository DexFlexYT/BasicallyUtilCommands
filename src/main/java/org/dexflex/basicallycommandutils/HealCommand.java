package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;

public class HealCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("heal")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("target", EntityArgumentType.entities())
                        .then(CommandManager.argument("amount", FloatArgumentType.floatArg(0))
                                .executes(HealCommand::execute)
                        )
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgumentType.getEntities(context, "target");
        float amount = FloatArgumentType.getFloat(context, "amount");

        int healed = 0;
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                float newHealth = Math.min(living.getHealth() + amount, living.getMaxHealth());
                living.setHealth(newHealth);
                healed++;
            }
        }

        final int healedFinal = healed;
        context.getSource().sendFeedback(
                () -> Text.literal("Healed " + healedFinal + " entities."),
                true
        );

        return healedFinal;
    }
}
