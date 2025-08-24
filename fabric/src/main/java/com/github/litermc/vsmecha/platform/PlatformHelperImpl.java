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

import com.google.auto.service.AutoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.ArgumentType;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.resource.conditions.v1.DefaultResourceConditions;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@AutoService(PlatformHelper.class)
public final class PlatformHelperImpl implements PlatformHelper {
	public static volatile MinecraftServer minecraftServer = null;

	@Override
	public boolean isDevelopmentEnvironment() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	@Override
	public ConfigFile.Builder createConfigBuilder() {
		return new FabricConfigFile.Builder();
	}

	@Override
	public MinecraftServer getCurrentServer() {
		return minecraftServer;
	}

	@Override
	public boolean isModLoaded(String modid) {
		return FabricLoader.getInstance().isModLoaded(modid);
	}

	@SuppressWarnings("unchecked")
	private static <T> Registry<T> getRegistry(ResourceKey<Registry<T>> id) {
		var registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(id.location());
		if (registry == null) throw new IllegalArgumentException("Unknown registry " + id);
		return registry;
	}

	@Override
	public <T> ResourceLocation getRegistryKey(ResourceKey<Registry<T>> registry, T object) {
		var key = getRegistry(registry).getKey(object);
		if (key == null) throw new IllegalArgumentException(object + " was not registered in " + registry);
		return key;
	}

	@Override
	public <T> T getRegistryObject(ResourceKey<Registry<T>> registry, ResourceLocation id) {
		var value = getRegistry(registry).get(id);
		if (value == null) throw new IllegalArgumentException(id + " was not registered in " + registry);
		return value;
	}

	@Override
	public <T> RegistryWrappers.RegistryWrapper<T> wrap(ResourceKey<Registry<T>> registry) {
		return new RegistryWrapperImpl<>(registry.location(), getRegistry(registry));
	}

	@Override
	public <T> RegistrationHelper<T> createRegistrationHelper(ResourceKey<Registry<T>> registry) {
		return new RegistrationHelperImpl<>(getRegistry(registry));
	}

	@Nullable
	@Override
	public <T> T tryGetRegistryObject(ResourceKey<Registry<T>> registry, ResourceLocation id) {
		return getRegistry(registry).get(id);
	}

	@Override
	public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
		final FabricBlockEntityTypeBuilder<T> builder = FabricBlockEntityTypeBuilder.create(factory::apply);
		for (final Block block : blocks) {
			builder.addBlock(block);
		}
		return builder.build();
	}

	@Override
	public <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> createMenuType(Function<FriendlyByteBuf, T> reader, ContainerData.Factory<C, T> factory) {
		return new ExtendedScreenHandlerType<>((id, player, data) -> factory.create(id, player, reader.apply(data)));
	}

	@Override
	public void openMenu(Player player, Component title, MenuConstructor menu, ContainerData data) {
		player.openMenu(new WrappedMenuProvider(title, menu, data));
	}

	@Override
	public <T extends NetworkMessage<?>> MessageType<T> createMessageType(int id, ResourceLocation channel, Class<T> klass, FriendlyByteBuf.Reader<T> reader) {
		return new FabricMessageType<>(channel, reader);
	}

	@Override
	public Packet<ClientGamePacketListener> createPacket(NetworkMessage<ClientNetworkContext> message) {
		var buf = PacketByteBufs.create();
		message.write(buf);
		return ServerPlayNetworking.createS2CPacket(FabricMessageType.toFabricType(message.type()).getId(), buf);
	}

	@Override
	public int getBurnTime(ItemStack stack) {
		var fuel = FuelRegistry.INSTANCE.get(stack.getItem());
		return fuel == null ? 0 : fuel;
	}

	@Override
	public CreativeModeTab.Builder newCreativeModeTab() {
		return FabricItemGroup.builder();
	}

	@Override
	public ServerPlayer createFakePlayer(ServerLevel world, GameProfile name) {
		return FakePlayer.create(world, name);
	}

	private record RegistryWrapperImpl<T>(
		ResourceLocation name, Registry<T> registry
	) implements RegistryWrappers.RegistryWrapper<T> {
		@Override
		public int getId(T object) {
			return registry.getId(object);
		}

		@Override
		public ResourceLocation getKey(T object) {
			var key = registry.getKey(object);
			if (key == null) throw new IllegalArgumentException(object + " was not registered in " + name);
			return key;
		}

		@Override
		public T get(ResourceLocation location) {
			var object = registry.get(location);
			if (object == null) throw new IllegalArgumentException(location + " was not registered in " + name);
			return object;
		}

		@Nullable
		@Override
		public T tryGet(ResourceLocation location) {
			return registry.get(location);
		}

		@Override
		public @Nullable T byId(int id) {
			return registry.byId(id);
		}

		@Override
		public int size() {
			return registry.size();
		}

		@Override
		public Iterator<T> iterator() {
			return registry.iterator();
		}
	}

	private static final class RegistrationHelperImpl<T> implements RegistrationHelper<T> {
		private final Registry<T> registry;
		private final List<RegistryEntryImpl<? extends T>> entries = new ArrayList<>();

		private RegistrationHelperImpl(Registry<T> registry) {
			this.registry = registry;
		}

		@Override
		public <U extends T> RegistryEntry<U> register(String name, Supplier<U> create) {
			var entry = new RegistryEntryImpl<>(new ResourceLocation(Constants.MOD_ID, name), create);
			entries.add(entry);
			return entry;
		}

		@Override
		public void register() {
			for (var entry : entries) entry.register(registry);
		}
	}

	private static final class RegistryEntryImpl<T> implements RegistryEntry<T> {
		private final ResourceLocation id;
		private final Supplier<T> supplier;
		private @Nullable T instance;

		RegistryEntryImpl(ResourceLocation id, Supplier<T> supplier) {
			this.id = id;
			this.supplier = supplier;
		}

		void register(Registry<? super T> registry) {
			Registry.register(registry, id, instance = supplier.get());
		}

		@Override
		public ResourceLocation id() {
			return id;
		}

		@Override
		public T get() {
			if (instance == null) throw new IllegalStateException(id + " has not been constructed yet");
			return instance;
		}
	}

	private record WrappedMenuProvider(
		Component title, MenuConstructor menu, ContainerData data
	) implements ExtendedScreenHandlerFactory {
		@Nullable
		@Override
		public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
			return menu.createMenu(id, inventory, player);
		}

		@Override
		public Component getDisplayName() {
			return title;
		}

		@Override
		public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
			data.toBytes(buf);
		}
	}
}
