package com.github.litermc.vsmecha;

import com.github.litermc.vsmecha.attachment.ToolCollisionAttachment;
import com.github.litermc.vsmecha.util.DestroyUtil;
import com.github.litermc.vsmecha.util.PredictUtil;
import com.github.litermc.vsmecha.util.TaskUtil;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public final class VSMechaListeners {
	private VSMechaListeners() {}

	public static void onServerLevelLoad(final ServerLevel level) {
	}

	public static void onServerLevelUnload(final ServerLevel level) {
	}

	public static void preServerTick(final MinecraftServer server) {
		TaskUtil.preServerTick();
	}

	public static void postServerTick(final MinecraftServer server) {
		TaskUtil.postServerTick();
		PredictUtil.postServerTick();
	}

	public static void postLevelTick(final ServerLevel level) {
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(level);
		final String dimId = VSGameUtilsKt.getDimensionId(level);
		for (final LoadedServerShip ship : world.getLoadedShips()) {
			if (!ship.getChunkClaimDimension().equals(dimId)) {
				continue;
			}
			final ToolCollisionAttachment attachment = ship.getAttachment(ToolCollisionAttachment.class);
			if (attachment == null) {
				continue;
			}
			attachment.tick(level, ship);
		}
		DestroyUtil.postTick(level);
	}
}
