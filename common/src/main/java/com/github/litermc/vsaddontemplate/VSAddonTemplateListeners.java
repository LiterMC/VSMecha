package com.github.litermc.vsaddontemplate;

import com.github.litermc.vsaddontemplate.util.TaskUtil;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class VSAddonTemplateListeners {
	private VSAddonTemplateListeners() {}

	public static void onServerLevelLoad(final ServerLevel level) {
	}

	public static void onServerLevelUnload(final ServerLevel level) {
	}

	public static void preServerTick(final MinecraftServer server) {
		TaskUtil.preServerTick();
	}

	public static void postServerTick(final MinecraftServer server) {
		TaskUtil.postServerTick();
	}
}
