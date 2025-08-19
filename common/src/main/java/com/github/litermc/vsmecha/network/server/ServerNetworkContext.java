// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vsmecha.network.server;

import net.minecraft.server.level.ServerPlayer;

/**
 * The context under which serverbound packets are evaluated.
 */
public interface ServerNetworkContext {
	/**
	 * Get the player who sent this packet.
	 *
	 * @return The sending player.
	 */
	ServerPlayer getSender();
}
