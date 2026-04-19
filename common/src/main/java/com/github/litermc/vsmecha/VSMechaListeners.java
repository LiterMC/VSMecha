package com.github.litermc.vsmecha;

import com.github.litermc.vsmecha.attachment.ShipNetworkAttachment;
import com.github.litermc.vsmecha.attachment.ToolCollisionAttachment;
import com.github.litermc.vsmecha.util.DestroyUtil;
import com.github.litermc.vsmecha.util.LevelUtil;
import com.github.litermc.vsmecha.util.PredictUtil;
import com.github.litermc.vsmecha.util.TaskUtil;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class VSMechaListeners {
	private VSMechaListeners() {}

	public static void onServerLevelLoad(final ServerLevel level) {
		LevelUtil.onServerLevelLoad(level);
	}

	public static void onServerLevelUnload(final ServerLevel level) {
		LevelUtil.onServerLevelUnload(level);
	}

	public static void preServerTick(final MinecraftServer server) {
		TaskUtil.preServerTick();
		ShipNetworkAttachment.preServerTick(server);
	}

	public static void postServerTick(final MinecraftServer server) {
		ShipNetworkAttachment.postServerTick(server);
		TaskUtil.postServerTick();
		PredictUtil.postServerTick();
	}

	public static void postLevelTick(final ServerLevel level) {
		ToolCollisionAttachment.postLevelTick(level);
		DestroyUtil.postTick(level);
	}
}
