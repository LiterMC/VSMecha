package com.github.litermc.vsmecha.util;

import com.github.litermc.vsmecha.platform.PlatformHelper;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;

import java.util.UUID;

public class TeamUtil {
	public static boolean isAlly(final UUID id1, final UUID id2) {
		if (id1 == null || id2 == null) {
			return false;
		}
		if (id1.equals(id2)) {
			return true;
		}
		MinecraftServer server = PlatformHelper.get().getCurrentServer();
		return false;
	}

	public static Team getPlayerTeam(MinecraftServer server, final UUID id) {
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		if (player == null) {
			final GameProfile profile = new GameProfile(id, "[vsmecha:TeamUtil]");
			player = new ServerPlayer(server, server.overworld(), profile);
			if (server.getPlayerList().load(player) == null) {
				return null;
			}
		}
		return player.getTeam();
	}
}
