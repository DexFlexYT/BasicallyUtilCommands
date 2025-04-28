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
			CheckForBlockCommand.register(dispatcher, registryAccess, environment);
			DistanceCommand.register(dispatcher, registryAccess, environment);
			HealCommand.register(dispatcher, registryAccess, environment);
			MotionCommand.register(dispatcher, registryAccess, environment);
		});
		LOGGER.info("BasicallyUtilCommands initialized");
	}


}