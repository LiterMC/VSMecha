// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vsmecha.platform;

import com.github.litermc.vsmecha.network.MessageType;
import com.github.litermc.vsmecha.network.NetworkMessage;
import com.github.litermc.vsmecha.network.client.ClientNetworkContext;
import com.github.litermc.vsmecha.network.container.ContainerData;
import com.github.litermc.vsmecha.config.ConfigFile;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nullable;

public interface PlatformHelper {
	/**
	 * Get the current {@link PlatformHelper} instance.
	 *
	 * @return The current instance.
	 */
	public static PlatformHelper get() {
		var instance = Instance.INSTANCE;
		return instance == null ? Services.raise(PlatformHelper.class, Instance.ERROR) : instance;
	}

	/**
	 * Check if we're running in a development environment.
	 *
	 * @return If we're running in a development environment.
	 */
	boolean isDevelopmentEnvironment();

	/**
	 * Create a new config builder.
	 *
	 * @return The newly created config builder.
	 */
	ConfigFile.Builder createConfigBuilder();

	MinecraftServer getCurrentServer();

	boolean isModLoaded(String modid);

	/**
	 * Wrap a Minecraft registry in our own abstraction layer.
	 *
	 * @param registry The registry to wrap.
	 * @param <T>      The type of object stored in this registry.
	 * @return The wrapped registry.
	 */
	<T> RegistryWrappers.RegistryWrapper<T> wrap(ResourceKey<Registry<T>> registry);

	/**
	 * Create a registration helper for a specific registry.
	 *
	 * @param registry The registry we'll add entries to.
	 * @param <T>      The type of object stored in the registry.
	 * @return The registration helper.
	 */
	<T> RegistrationHelper<T> createRegistrationHelper(ResourceKey<Registry<T>> registry);

	/**
	 * A version of {@link #getRegistryObject(ResourceKey, ResourceLocation)} which allows missing entries.
	 *
	 * @param registry The registry to look up this object in.
	 * @param id       The ID to look up.
	 * @param <T>      The type of object the registry stores.
	 * @return The registered object or {@code null}.
	 */
	@Nullable
	<T> T tryGetRegistryObject(ResourceKey<Registry<T>> registry, ResourceLocation id);

	/**
	 * Get the unique ID for a registered object.
	 *
	 * @param registry The registry to look up this object in.
	 * @param object   The object to look up.
	 * @param <T>      The type of object the registry stores.
	 * @return The registered object's ID.
	 * @throws IllegalArgumentException If the registry or object are not registered.
	 */
	<T> ResourceLocation getRegistryKey(ResourceKey<Registry<T>> registry, T object);

	/**
	 * Look up an ID in a registry, returning the registered object.
	 *
	 * @param registry The registry to look up this object in.
	 * @param id       The ID to look up.
	 * @param <T>      The type of object the registry stores.
	 * @return The resolved registry object.
	 * @throws IllegalArgumentException If the registry or object are not registered.
	 */
	<T> T getRegistryObject(ResourceKey<Registry<T>> registry, ResourceLocation id);

	/**
	 * Create a new block entity type which serves a particular block.
	 *
	 * @param factory The method which creates a new block entity with this type, typically the constructor.
	 * @param block   The block this block entity exists on.
	 * @param <T>     The type of block entity we're creating.
	 * @return The new block entity type.
	 */
	<T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block block);

	/**
	 * Create a menu type which sends additional data when opened.
	 *
	 * @param reader  Parse the additional container data into a usable type.
	 * @param factory The factory to create the new menu.
	 * @param <C>     The menu/container than we open.
	 * @param <T>     The data that we send to the client.
	 * @return The menu type for this container.
	 */
	<C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> createMenuType(Function<FriendlyByteBuf, T> reader, ContainerData.Factory<C, T> factory);

	/**
	 * Open a container using a specific {@link ContainerData}.
	 *
	 * @param player The player to open the menu for.
	 * @param title  The title for this menu.
	 * @param menu   The underlying menu constructor.
	 * @param data   The menu data.
	 */
	void openMenu(Player player, Component title, MenuConstructor menu, ContainerData data);

	/**
	 * Create a new {@link MessageType}.
	 *
	 * @param id      The descriminator for this message type.
	 * @param channel The channel name for this message type.
	 * @param klass   The type of this message.
	 * @param reader  The function which reads the packet from a buffer. Should be the inverse to {@link NetworkMessage#write(FriendlyByteBuf)}.
	 * @param <T>     The type of this message.
	 * @return The new {@link MessageType} instance.
	 */
	<T extends NetworkMessage<?>> MessageType<T> createMessageType(int id, ResourceLocation channel, Class<T> klass, FriendlyByteBuf.Reader<T> reader);

	/**
	 * Convert a clientbound {@link NetworkMessage} to a Minecraft {@link Packet}.
	 *
	 * @param message The messsge to convert.
	 * @return The converted message.
	 */
	Packet<ClientGamePacketListener> createPacket(NetworkMessage<ClientNetworkContext> message);

	/**
	 * Create a builder for a new creative tab.
	 *
	 * @return The creative tab builder.
	 */
	CreativeModeTab.Builder newCreativeModeTab();

	/**
	* Create a new fake player.
	*
	* @param level   The level the player should be created in.
	* @param profile The user this player should mimic.
	* @return The newly constructed fake player.
	*/
	ServerPlayer createFakePlayer(ServerLevel level, GameProfile profile);

	/**
	* Determine if a player is not a real player.
	*
	* @param player The player to check.
	* @return Whether this player is fake.
	*/
	default boolean isFakePlayer(ServerPlayer player) {
		// Any subclass of ServerPlayer (i.e. Forge's FakePlayer) is assumed to be a fake.
		return player.connection == null || player.getClass() != ServerPlayer.class;
	}

	final class Instance {
		static final @Nullable PlatformHelper INSTANCE;
		static final @Nullable Throwable ERROR;

		static {
			// We don't want class initialisation to fail here (as that results in confusing errors). Instead, capture
			// the error and rethrow it when accessing. This should be JITted away in the common case.
			var helper = Services.tryLoad(PlatformHelper.class);
			INSTANCE = helper.instance();
			ERROR = helper.error();
		}

		private Instance() {
		}
	}
}
