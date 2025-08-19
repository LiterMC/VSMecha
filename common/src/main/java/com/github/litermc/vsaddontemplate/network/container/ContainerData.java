// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vsaddontemplate.network.container;

import com.github.litermc.vsaddontemplate.network.NetworkMessage;
import com.github.litermc.vsaddontemplate.platform.PlatformHelper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Function;

/**
 * Additional data to send when opening a menu. Like {@link NetworkMessage}, this should be immutable.
 */
public interface ContainerData {
	void toBytes(FriendlyByteBuf buf);

	static <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> toType(Function<FriendlyByteBuf, T> reader, Factory<C, T> factory) {
		return PlatformHelper.get().createMenuType(reader, factory);
	}

	interface Factory<C extends AbstractContainerMenu, T extends ContainerData> {
		C create(int id, Inventory inventory, T data);
	}
}
