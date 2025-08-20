// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vsmecha.platform;

import com.github.litermc.vsmecha.Constants;
import com.github.litermc.vsmecha.config.ConfigFile;
import com.github.litermc.vsmecha.network.MessageType;
import com.github.litermc.vsmecha.network.NetworkMessage;
import com.github.litermc.vsmecha.network.client.ClientNetworkContext;
import com.github.litermc.vsmecha.network.container.ContainerData;
import com.github.litermc.vsmecha.util.TaskUtil;

import com.google.auto.service.AutoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@AutoService(PlatformHelper.class)
public class PlatformHelperImpl implements PlatformHelper {
	@Override
	public boolean isDevelopmentEnvironment() {
		return !FMLLoader.isProduction();
	}

	@Override
	public ConfigFile.Builder createConfigBuilder() {
		return new ForgeConfigFile.Builder();
	}

	@Override
	public MinecraftServer getCurrentServer() {
		return ServerLifecycleHooks.getCurrentServer();
	}

	@Override
	public boolean isModLoaded(String modid) {
		return ModList.get().isLoaded(modid);
	}

	@Override
	public <T> ResourceLocation getRegistryKey(ResourceKey<Registry<T>> registry, T object) {
		var key = RegistryManager.ACTIVE.getRegistry(registry).getKey(object);
		if (key == null) throw new IllegalArgumentException(object + " was not registered in " + registry);
		return key;
	}

	@Override
	public <T> T getRegistryObject(ResourceKey<Registry<T>> registry, ResourceLocation id) {
		var value = RegistryManager.ACTIVE.getRegistry(registry).getValue(id);
		if (value == null) throw new IllegalArgumentException(id + " was not registered in " + registry);
		return value;
	}

	@Override
	public <T> RegistryWrappers.RegistryWrapper<T> wrap(ResourceKey<Registry<T>> key) {
		return new RegistryWrapperImpl<>(key.location(), RegistryManager.ACTIVE.getRegistry(key));
	}

	@Override
	public <T> RegistrationHelper<T> createRegistrationHelper(ResourceKey<Registry<T>> registry) {
		return new RegistrationHelperImpl<>(DeferredRegister.create(registry, Constants.MOD_ID));
	}

	@Nullable
	@Override
	public <K> K tryGetRegistryObject(ResourceKey<Registry<K>> registry, ResourceLocation id) {
		return RegistryManager.ACTIVE.getRegistry(registry).getValue(id);
	}

	@Override
	public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
		return new BlockEntityType<>(factory::apply, Set.of(blocks), null);
	}

	@Override
	public <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> createMenuType(Function<FriendlyByteBuf, T> reader, ContainerData.Factory<C, T> factory) {
		return IForgeMenuType.create((id, player, data) -> factory.create(id, player, reader.apply(data)));
	}

	@Override
	public void openMenu(Player player, Component title, MenuConstructor menu, ContainerData data) {
		NetworkHooks.openScreen((ServerPlayer) player, new SimpleMenuProvider(menu, title), data::toBytes);
	}

	@Override
	public <T extends NetworkMessage<?>> MessageType<T> createMessageType(int id, ResourceLocation channel, Class<T> klass, FriendlyByteBuf.Reader<T> reader) {
		return new NetworkHandler.MessageTypeImpl<>(id, klass, reader);
	}

	@Override
	public Packet<ClientGamePacketListener> createPacket(NetworkMessage<ClientNetworkContext> message) {
		return NetworkHandler.createClientboundPacket(message);
	}

	@Override
	public CreativeModeTab.Builder newCreativeModeTab() {
		return CreativeModeTab.builder();
	}

	@Override
	public ServerPlayer createFakePlayer(ServerLevel world, GameProfile profile) {
		return FakePlayer.create(world, profile);
	}

	private record RegistryWrapperImpl<T>(
		ResourceLocation name, ForgeRegistry<T> registry
	) implements RegistryWrappers.RegistryWrapper<T> {
		@Override
		public int getId(T object) {
			return registry.getID(object);
		}

		@Override
		public ResourceLocation getKey(T object) {
			var key = registry.getKey(object);
			if (key == null) throw new IllegalStateException(object + " was not registered in " + name);
			return key;
		}

		@Override
		public T get(ResourceLocation location) {
			var object = registry.getValue(location);
			if (object == null) throw new IllegalStateException(location + " was not registered in " + name);
			return object;
		}

		@Nullable
		@Override
		public T tryGet(ResourceLocation location) {
			return registry.getValue(location);
		}

		@Override
		public @Nullable T byId(int id) {
			return registry.getValue(id);
		}

		@Override
		public int size() {
			return registry.getKeys().size();
		}

		@Override
		public Iterator<T> iterator() {
			return registry.iterator();
		}
	}

	private record RegistrationHelperImpl<T>(DeferredRegister<T> registry) implements RegistrationHelper<T> {
		@Override
		public <U extends T> RegistryEntry<U> register(String name, Supplier<U> create) {
			return new RegistryEntryImpl<>(registry().register(name, create));
		}

		@Override
		public void register() {
			registry().register(FMLJavaModLoadingContext.get().getModEventBus());
		}
	}

	private record RegistryEntryImpl<T>(RegistryObject<T> object) implements RegistryEntry<T> {
		@Override
		public ResourceLocation id() {
			return object().getId();
		}

		@Override
		public T get() {
			return object().get();
		}
	}
}
