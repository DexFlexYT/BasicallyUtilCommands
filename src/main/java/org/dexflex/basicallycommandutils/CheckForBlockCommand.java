package org.dexflex.basicallycommandutils;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;

public class CheckForBlockCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, RegistrationEnvironment environment) {
		dispatcher.register(CommandManager.literal("checkforblock")
				.requires(source -> source.hasPermissionLevel(4))
				.then(CommandManager.argument("block", BlockStateArgumentType.blockState(registryAccess))
						.then(CommandManager.argument("distance", IntegerArgumentType.integer(1))
								.executes(CheckForBlockCommand::execute)
						)
				)
		);
	}
	private static int execute(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		ServerWorld world = source.getWorld();
		BlockPos origin = BlockPos.ofFloored(source.getPosition());

		BlockStateArgument blockArg = BlockStateArgumentType.getBlockState(context, "block");
		int distance = IntegerArgumentType.getInteger(context, "distance");

		int count = 0;

		for (int x = -distance; x <= distance; x++) {
			for (int y = -distance; y <= distance; y++) {
				for (int z = -distance; z <= distance; z++) {
					BlockPos pos = origin.add(x, y, z);
					if (blockArg.test(new CachedBlockPosition(world, pos, true))) {
						count++;
					}
				}
			}
		}
		String message = "Found " + count + " matching blocks.";
		source.sendFeedback(() -> Text.literal(message), false);
		return count;
	}
}
