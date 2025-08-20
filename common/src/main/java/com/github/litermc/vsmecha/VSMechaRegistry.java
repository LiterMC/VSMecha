// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vsmecha;

import com.github.litermc.vsmecha.block.StainedToolBlock;
import com.github.litermc.vsmecha.block.ToolBaseBlockEntity;
import com.github.litermc.vsmecha.platform.PlatformHelper;
import com.github.litermc.vsmecha.platform.RegistrationHelper;
import com.github.litermc.vsmecha.platform.RegistryEntry;
import com.github.litermc.vsmecha.shape.IToolShape;
import com.github.litermc.vsmecha.shape.SwordShape;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public final class VSMechaRegistry {
	private VSMechaRegistry() {}

	private static final Set<IToolShape> TOOL_SHAPE_SET = new HashSet<>();
	public static final Collection<IToolShape> TOOL_SHAPES = Collections.unmodifiableCollection(TOOL_SHAPE_SET);
	static {
		registerToolShape(SwordShape.INSTANCE);
	}

	public static void registerToolShape(final IToolShape shape) {
		TOOL_SHAPE_SET.add(shape);
	}

	public static void register() {
		Blocks.REGISTRY.register();
		BlockEntities.REGISTRY.register();
		Items.REGISTRY.register();
		CreativeTabs.REGISTRY.register();
	}

	public static final class Blocks {
		private static final RegistrationHelper<Block> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.BLOCK);

		public static final RegistryEntry<StainedToolBlock> WHITE_TOOL_BLOCK =
			REGISTRY.register("white_tool_block", () -> new StainedToolBlock(DyeColor.WHITE, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WHITE_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> ORANGE_TOOL_BLOCK =
			REGISTRY.register("orange_tool_block", () -> new StainedToolBlock(DyeColor.ORANGE, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.ORANGE_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> MAGENTA_TOOL_BLOCK =
			REGISTRY.register("magenta_tool_block", () -> new StainedToolBlock(DyeColor.MAGENTA, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.MAGENTA_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> LIGHT_BLUE_TOOL_BLOCK =
			REGISTRY.register("light_blue_tool_block", () -> new StainedToolBlock(DyeColor.LIGHT_BLUE, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.LIGHT_BLUE_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> YELLOW_TOOL_BLOCK =
			REGISTRY.register("yellow_tool_block", () -> new StainedToolBlock(DyeColor.YELLOW, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.YELLOW_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> LIME_TOOL_BLOCK =
			REGISTRY.register("lime_tool_block", () -> new StainedToolBlock(DyeColor.LIME, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.LIME_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> PINK_TOOL_BLOCK =
			REGISTRY.register("pink_tool_block", () -> new StainedToolBlock(DyeColor.PINK, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.PINK_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> GRAY_TOOL_BLOCK =
			REGISTRY.register("gray_tool_block", () -> new StainedToolBlock(DyeColor.GRAY, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.GRAY_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> LIGHT_GRAY_TOOL_BLOCK =
			REGISTRY.register("light_gray_tool_block", () -> new StainedToolBlock(DyeColor.LIGHT_GRAY, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.LIGHT_GRAY_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> CYAN_TOOL_BLOCK =
			REGISTRY.register("cyan_tool_block", () -> new StainedToolBlock(DyeColor.CYAN, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.CYAN_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> PURPLE_TOOL_BLOCK =
			REGISTRY.register("purple_tool_block", () -> new StainedToolBlock(DyeColor.PURPLE, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.PURPLE_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> BLUE_TOOL_BLOCK =
			REGISTRY.register("blue_tool_block", () -> new StainedToolBlock(DyeColor.BLUE, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.BLUE_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> BROWN_TOOL_BLOCK =
			REGISTRY.register("brown_tool_block", () -> new StainedToolBlock(DyeColor.BROWN, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.BROWN_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> GREEN_TOOL_BLOCK =
			REGISTRY.register("green_tool_block", () -> new StainedToolBlock(DyeColor.GREEN, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.GREEN_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> RED_TOOL_BLOCK =
			REGISTRY.register("red_tool_block", () -> new StainedToolBlock(DyeColor.RED, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.RED_CONCRETE)));
		public static final RegistryEntry<StainedToolBlock> BLACK_TOOL_BLOCK =
			REGISTRY.register("black_tool_block", () -> new StainedToolBlock(DyeColor.BLACK, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.BLACK_CONCRETE)));

		public static void onRegisterRenderType(final BiConsumer<Block, RenderType> consumer) {
		}

		private Blocks() {}
	}

	public static final class BlockEntities {
		private static final RegistrationHelper<BlockEntityType<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.BLOCK_ENTITY_TYPE);

		private static <T extends BlockEntity> RegistryEntry<BlockEntityType<T>> of(final String id, final BiFunction<BlockPos, BlockState, T> factory, final RegistryEntry<? extends Block>... blocks) {
			return REGISTRY.register(id, () -> {
				final Block[] blks = new Block[blocks.length];
				for (int i = 0; i < blocks.length; i++) {
					blks[i] = blocks[i].get();
				}
				return PlatformHelper.get().createBlockEntityType(factory, blks);
			});
		}

		public static final RegistryEntry<BlockEntityType<ToolBaseBlockEntity>> TOOL_BASE =
			of("tool_base", ToolBaseBlockEntity::new,
				Blocks.WHITE_TOOL_BLOCK,
				Blocks.ORANGE_TOOL_BLOCK,
				Blocks.MAGENTA_TOOL_BLOCK,
				Blocks.LIGHT_BLUE_TOOL_BLOCK,
				Blocks.YELLOW_TOOL_BLOCK,
				Blocks.LIME_TOOL_BLOCK,
				Blocks.PINK_TOOL_BLOCK,
				Blocks.GRAY_TOOL_BLOCK,
				Blocks.LIGHT_GRAY_TOOL_BLOCK,
				Blocks.CYAN_TOOL_BLOCK,
				Blocks.PURPLE_TOOL_BLOCK,
				Blocks.BLUE_TOOL_BLOCK,
				Blocks.BROWN_TOOL_BLOCK,
				Blocks.GREEN_TOOL_BLOCK,
				Blocks.RED_TOOL_BLOCK,
				Blocks.BLACK_TOOL_BLOCK
			);

		private BlockEntities() {}
	}

	public static final class Items {
		private static final RegistrationHelper<Item> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.ITEM);
		private static final List<RegistryEntry<? extends Item>> TAB_ITEMS = new ArrayList<>();

		private static Item.Properties properties() {
			return new Item.Properties();
		}

		private static <B extends Block, I extends Item> RegistryEntry<I> ofBlockNoTab(RegistryEntry<B> block, BiFunction<B, Item.Properties, I> supplier) {
			return REGISTRY.register(block.id().getPath(), () -> supplier.apply(block.get(), properties()));
		}

		private static <B extends Block, I extends Item> RegistryEntry<I> ofBlock(RegistryEntry<B> block, BiFunction<B, Item.Properties, I> supplier) {
			final RegistryEntry<I> entry = ofBlockNoTab(block, supplier);
			TAB_ITEMS.add(entry);
			return entry;
		}

		public static final RegistryEntry<BlockItem> WHITE_TOOL_BLOCK = ofBlock(
			Blocks.WHITE_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> ORANGE_TOOL_BLOCK = ofBlock(
			Blocks.ORANGE_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> MAGENTA_TOOL_BLOCK = ofBlock(
			Blocks.MAGENTA_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> LIGHT_BLUE_TOOL_BLOCK = ofBlock(
			Blocks.LIGHT_BLUE_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> YELLOW_TOOL_BLOCK = ofBlock(
			Blocks.YELLOW_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> LIME_TOOL_BLOCK = ofBlock(
			Blocks.LIME_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> PINK_TOOL_BLOCK = ofBlock(
			Blocks.PINK_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> GRAY_TOOL_BLOCK = ofBlock(
			Blocks.GRAY_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> LIGHT_GRAY_TOOL_BLOCK = ofBlock(
			Blocks.LIGHT_GRAY_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> CYAN_TOOL_BLOCK = ofBlock(
			Blocks.CYAN_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> PURPLE_TOOL_BLOCK = ofBlock(
			Blocks.PURPLE_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> BLUE_TOOL_BLOCK = ofBlock(
			Blocks.BLUE_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> BROWN_TOOL_BLOCK = ofBlock(
			Blocks.BROWN_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> GREEN_TOOL_BLOCK = ofBlock(
			Blocks.GREEN_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> RED_TOOL_BLOCK = ofBlock(
			Blocks.RED_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> BLACK_TOOL_BLOCK = ofBlock(
			Blocks.BLACK_TOOL_BLOCK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);

		private Items() {}
	}

	static class CreativeTabs {
		static final RegistrationHelper<CreativeModeTab> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.CREATIVE_MODE_TAB);

		private static final RegistryEntry<CreativeModeTab> TAB = REGISTRY.register(
			"tab",
			() -> PlatformHelper.get().newCreativeModeTab()
				// .icon(() -> new ItemStack(Items.CHUNK_LOADER.get()))
				.title(Component.translatable("itemGroup." + Constants.MOD_ID))
				.displayItems((context, out) -> {
					Items.TAB_ITEMS.stream().map(RegistryEntry::get).forEach(out::accept);
				})
				.build()
		);
	}
}
