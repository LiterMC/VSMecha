package com.github.litermc.vsmecha;

import com.github.litermc.vsmecha.block.BlockCapabilityProviders;
import com.github.litermc.vsmecha.command.VSMechaCommands;
import com.github.litermc.vsmecha.config.ConfigSpec;
import com.github.litermc.vsmecha.platform.FabricConfigFile;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.world.level.storage.LevelResource;

public class ModEntry implements ModInitializer {
	private static final String SERVERCONFIG = "serverconfig";

	@Override
	public void onInitialize() {
		VSMechaListeners.onModInit();
		BlockCapabilityProviders.register();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> VSMechaCommands.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
			((FabricConfigFile)(ConfigSpec.serverSpec))
				.load(server.getWorldPath(LevelResource.ROOT).resolve(SERVERCONFIG).resolve(Constants.MOD_ID + "-server.toml"));
		});

		ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
			((FabricConfigFile)(ConfigSpec.serverSpec)).unload();
		});

		ServerWorldEvents.LOAD.register((server, level) -> {
			VSMechaListeners.onServerLevelLoad(level);
		});

		ServerWorldEvents.UNLOAD.register((server, level) -> {
			VSMechaListeners.onServerLevelUnload(level);
		});

		ServerTickEvents.START_SERVER_TICK.register(VSMechaListeners::preServerTick);
		ServerTickEvents.END_SERVER_TICK.register(VSMechaListeners::postServerTick);
		ServerTickEvents.END_WORLD_TICK.register(VSMechaListeners::postLevelTick);

		VSMechaListeners.onModSetup();
	}
}
