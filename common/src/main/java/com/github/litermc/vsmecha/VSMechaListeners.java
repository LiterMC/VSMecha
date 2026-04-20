package com.github.litermc.vsmecha;

import com.github.litermc.vsmecha.attachment.ShipNetworkAttachment;
import com.github.litermc.vsmecha.attachment.ToolCollisionAttachment;
import com.github.litermc.vsmecha.util.DestroyUtil;
import com.github.litermc.vsmecha.util.LevelUtil;
import com.github.litermc.vsmecha.util.PredictUtil;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

public final class VSMechaListeners {
	private VSMechaListeners() {}

	/**
	 * may runs parallelly with other mods
	 */
	public static void onModInit() {
		VSMechaRegistry.register();
	}

	/**
	 * runs on main thread only
	 */
	public static void onModSetup() {
		registerAttachments();
	}

	private static void registerAttachments() {
		ValkyrienSkiesMod.getApi().registerAttachment(ShipNetworkAttachment.class);
		ValkyrienSkiesMod.getApi().registerAttachment(ToolCollisionAttachment.class);
	}

	public static void onServerLevelLoad(final ServerLevel level) {
		LevelUtil.onServerLevelLoad(level);
	}

	public static void onServerLevelUnload(final ServerLevel level) {
		LevelUtil.onServerLevelUnload(level);
	}

	public static void preServerTick(final MinecraftServer server) {
		ShipNetworkAttachment.preServerTick(server);
	}

	public static void postServerTick(final MinecraftServer server) {
		ShipNetworkAttachment.postServerTick(server);
		PredictUtil.postServerTick();
	}

	public static void postLevelTick(final ServerLevel level) {
		DestroyUtil.postTick(level);
	}
}
