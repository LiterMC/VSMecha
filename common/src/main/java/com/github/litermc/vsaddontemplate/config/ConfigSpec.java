package com.github.litermc.vsaddontemplate.config;

import com.github.litermc.vsaddontemplate.platform.PlatformHelper;

import java.nio.file.Path;

public final class ConfigSpec {
	public static final ConfigFile serverSpec;

	// public static final ConfigFile.Value<Boolean> FORCE_LOAD_ALL_SHIPS;

	private ConfigSpec() {}

	static {
		final ConfigFile.Builder builder = PlatformHelper.get().createConfigBuilder();
		// {
		// 	builder
		// 		.comment("General settings")
		// 		.push("general");

		// 	FORCE_LOAD_ALL_SHIPS = builder
		// 		.comment("Should force load all ships on the server")
		// 		.define("force_load_all_ships", Config.forceLoadAllShips);

		// 	builder.pop();
		// }

		serverSpec = builder.build(ConfigSpec::syncServer);
	}

	public static void syncServer(Path path) {
		// Config.forceLoadAllShips = FORCE_LOAD_ALL_SHIPS.get();
	}

	public static void syncClient(Path path) {
	}
}
