package org.dexflex.basicallycommandutils;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicallyUtilCommands implements ModInitializer {
	public static final String MOD_ID = "basicallyutilcommands";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			RaycastCommand.register(dispatcher);
			CheckCommand.register(dispatcher, registryAccess);
			DistanceCommand.register(dispatcher);
			HealCommand.register(dispatcher);
			MotionCommand.register(dispatcher);
			IgniteCommand.register(dispatcher);
			EntitifyCommand.register(dispatcher);
			RandCommand.register(dispatcher);
		});
		LOGGER.info("BasicallyUtilCommands initialized");
	}
}