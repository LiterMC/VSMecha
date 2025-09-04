// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vsmecha;

import com.github.litermc.vsmecha.block.control.CapsuleHeadBlock;
import com.github.litermc.vsmecha.block.control.CapsuleSeatBlock;
import com.github.litermc.vsmecha.block.control.CapsuleSeatBlockEntity;
import com.github.litermc.vsmecha.block.energy.EnergyPortBlock;
import com.github.litermc.vsmecha.block.energy.EnergyPortBlockEntity;
import com.github.litermc.vsmecha.block.energy.PlasmaCapacitorBlock;
import com.github.litermc.vsmecha.block.energy.PlasmaCapacitorBlockEntity;
import com.github.litermc.vsmecha.block.energy.ThermalEnergyCoreBlock;
import com.github.litermc.vsmecha.block.energy.ThermalEnergyCoreBlockEntity;
import com.github.litermc.vsmecha.block.joint.ElectroGraspBlock;
import com.github.litermc.vsmecha.block.joint.ElectroGraspBlockEntity;
import com.github.litermc.vsmecha.block.joint.ServoBlock;
import com.github.litermc.vsmecha.block.joint.ServoBlockEntity;
import com.github.litermc.vsmecha.block.joint.ServoHeadBlock;
import com.github.litermc.vsmecha.block.joint.ServoHeadBlockEntity;
import com.github.litermc.vsmecha.block.radar.IFFBeaconBlock;
import com.github.litermc.vsmecha.block.radar.IFFBeaconBlockEntity;
import com.github.litermc.vsmecha.block.radar.IRSensorBlockEntity;
import com.github.litermc.vsmecha.block.radar.RadarBlock;
import com.github.litermc.vsmecha.block.tool.StainedToolBlock;
import com.github.litermc.vsmecha.block.tool.ToolBaseBlockEntity;
import com.github.litermc.vsmecha.entity.SeatEntity;
import com.github.litermc.vsmecha.platform.PlatformHelper;
import com.github.litermc.vsmecha.platform.RegistrationHelper;
import com.github.litermc.vsmecha.platform.RegistryEntry;
import com.github.litermc.vsmecha.shape.IToolShape;
import com.github.litermc.vsmecha.shape.PickAxeShape;
import com.github.litermc.vsmecha.shape.SwordShape;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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
import java.util.function.Supplier;

public final class VSMechaRegistry {
	private VSMechaRegistry() {}

	private static final Set<IToolShape> TOOL_SHAPE_SET = new HashSet<>();
	public static final Collection<IToolShape> TOOL_SHAPES = Collections.unmodifiableCollection(TOOL_SHAPE_SET);
	static {
		registerToolShape(PickAxeShape.INSTANCE);
		registerToolShape(SwordShape.INSTANCE);
	}

	public static void registerToolShape(final IToolShape shape) {
		TOOL_SHAPE_SET.add(shape);
	}

	public static void register() {
		Blocks.REGISTRY.register();
		BlockEntities.REGISTRY.register();
		Items.REGISTRY.register();
		Entities.REGISTRY.register();
		CreativeTabs.REGISTRY.register();
	}

	public static final class Blocks {
		private static final RegistrationHelper<Block> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.BLOCK);

		private static BlockBehaviour.Properties properties() {
			return BlockBehaviour.Properties.of().isValidSpawn((state, level, pos, entityType) -> false);
		}

		public static final RegistryEntry<CapsuleHeadBlock> CAPSULE_HEAD = REGISTRY.register("capsule_head", () -> new CapsuleHeadBlock(
			properties()
				.noOcclusion()
				.isRedstoneConductor((state, level, pos) -> false)
				.isSuffocating((state, level, pos) -> false)
				.isViewBlocking((state, level, pos) -> false)
				.requiresCorrectToolForDrops()
		));
		public static final RegistryEntry<CapsuleSeatBlock> CAPSULE_SEAT = REGISTRY.register("capsule_seat", () -> new CapsuleSeatBlock(
			properties()
				.isRedstoneConductor((state, level, pos) -> false)
				.requiresCorrectToolForDrops()
		));

		public static final RegistryEntry<EnergyPortBlock> ENERGY_PORT = REGISTRY.register("energy_port", () -> new EnergyPortBlock(properties()));
		public static final RegistryEntry<PlasmaCapacitorBlock> PLASMA_CAPACITOR = REGISTRY.register("plasma_capacitor", () -> new PlasmaCapacitorBlock(properties()));
		public static final RegistryEntry<ThermalEnergyCoreBlock> THERMAL_ENERGY_CORE = REGISTRY.register("thermal_energy_core", () -> new ThermalEnergyCoreBlock(properties()));

		public static final RegistryEntry<ElectroGraspBlock> ELECTRO_GRASP = REGISTRY.register("electro_grasp", () -> new ElectroGraspBlock(properties()));
		public static final RegistryEntry<ServoBlock> SERVO = REGISTRY.register("servo", () -> new ServoBlock(properties()));
		public static final RegistryEntry<ServoHeadBlock> SERVO_HEAD = REGISTRY.register("servo_head", () -> new ServoHeadBlock(properties().noCollission()));

		public static final RegistryEntry<IFFBeaconBlock> IFF_BEACON = REGISTRY.register("iff_beacon", () -> new IFFBeaconBlock(properties()));
		public static final RegistryEntry<RadarBlock> IR_SENSOR = REGISTRY.register("ir_sensor", () -> new RadarBlock(properties()) {
			@Override
			public IRSensorBlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
				return new IRSensorBlockEntity(pos, state);
			}
		});

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
			if (blocks.length == 0) {
				throw new AssertionError("No block assigned to block entity " + id);
			}
			return REGISTRY.register(id, () -> {
				final Block[] blks = new Block[blocks.length];
				for (int i = 0; i < blocks.length; i++) {
					blks[i] = blocks[i].get();
				}
				return PlatformHelper.get().createBlockEntityType(factory, blks);
			});
		}

		public static final RegistryEntry<BlockEntityType<CapsuleSeatBlockEntity>> CAPSULE_SEAT = of("capsule_seat", CapsuleSeatBlockEntity::new, Blocks.CAPSULE_SEAT);

		public static final RegistryEntry<BlockEntityType<EnergyPortBlockEntity>> ENERGY_PORT = of("energy_port", EnergyPortBlockEntity::new, Blocks.ENERGY_PORT);
		public static final RegistryEntry<BlockEntityType<PlasmaCapacitorBlockEntity>> PLASMA_CAPACITOR = of("plasma_capacitor", PlasmaCapacitorBlockEntity::new, Blocks.PLASMA_CAPACITOR);
		public static final RegistryEntry<BlockEntityType<ThermalEnergyCoreBlockEntity>> THERMAL_ENERGY_CORE = of("thermal_energy_core", ThermalEnergyCoreBlockEntity::new, Blocks.THERMAL_ENERGY_CORE);

		public static final RegistryEntry<BlockEntityType<ElectroGraspBlockEntity>> ELECTRO_GRASP = of("electro_grasp", ElectroGraspBlockEntity::new, Blocks.ELECTRO_GRASP);
		public static final RegistryEntry<BlockEntityType<ServoBlockEntity>> SERVO = of("servo", ServoBlockEntity::new, Blocks.SERVO);
		public static final RegistryEntry<BlockEntityType<ServoHeadBlockEntity>> SERVO_HEAD = of("servo_head", ServoHeadBlockEntity::new, Blocks.SERVO_HEAD);

		public static final RegistryEntry<BlockEntityType<IFFBeaconBlockEntity>> IFF_BEACON = of("iff_beacon", IFFBeaconBlockEntity::new, Blocks.IFF_BEACON);
		public static final RegistryEntry<BlockEntityType<IRSensorBlockEntity>> IR_SENSOR = of("ir_sensor", IRSensorBlockEntity::new, Blocks.IR_SENSOR);

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

		public static final RegistryEntry<BlockItem> CAPSULE_HEAD = ofBlock(
			Blocks.CAPSULE_HEAD,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.RARE).stacksTo(1))
		);
		public static final RegistryEntry<BlockItem> CAPSULE_SEAT = ofBlock(
			Blocks.CAPSULE_SEAT,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(1))
		);

		public static final RegistryEntry<BlockItem> ENERGY_PORT = ofBlock(
			Blocks.ENERGY_PORT,
			(block, props) -> new BlockItem(block, props)
		);
		public static final RegistryEntry<BlockItem> PLASMA_CAPACITOR = ofBlock(
			Blocks.PLASMA_CAPACITOR,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.EPIC).stacksTo(1))
		);
		public static final RegistryEntry<BlockItem> THERMAL_ENERGY_CORE = ofBlock(
			Blocks.THERMAL_ENERGY_CORE,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON))
		);

		public static final RegistryEntry<BlockItem> ELECTRO_GRASP = ofBlock(
			Blocks.ELECTRO_GRASP,
			(block, props) -> new BlockItem(block, props)
		);
		public static final RegistryEntry<BlockItem> SERVO = ofBlock(
			Blocks.SERVO,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.RARE).stacksTo(16))
		);
		public static final RegistryEntry<BlockItem> SERVO_HEAD = ofBlock(
			Blocks.SERVO_HEAD,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON))
		);

		public static final RegistryEntry<BlockItem> IFF_BEACON = ofBlock(
			Blocks.IFF_BEACON,
			(block, props) -> new BlockItem(block, props.stacksTo(1))
		);
		public static final RegistryEntry<BlockItem> IR_SENSOR = ofBlock(
			Blocks.IR_SENSOR,
			(block, props) -> new BlockItem(block, props)
		);

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

	public static final class Entities {
		private static final RegistrationHelper<EntityType<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.ENTITY_TYPE);

		private static <T extends Entity> RegistryEntry<EntityType<T>> of(final String name, final Supplier<EntityType.Builder<T>> supplier) {
			return REGISTRY.register(name, () -> supplier.get().build(new ResourceLocation(Constants.MOD_ID, name).toString()));
		}

		public static final RegistryEntry<EntityType<SeatEntity>> SEAT = of(
			"seat",
			() -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
				.sized(0, 0)
				.noSummon()
				.canSpawnFarFromPlayer()
				.clientTrackingRange(32)
				.updateInterval(Integer.MAX_VALUE)
		);

		public static void onRegisterEntityRender(final EntityRendererRegister register) {
			register.accept(SEAT.get(), SeatEntity.Renderer::new);
		}

		private Entities() {}
	}

	static class CreativeTabs {
		static final RegistrationHelper<CreativeModeTab> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.CREATIVE_MODE_TAB);

		private static final RegistryEntry<CreativeModeTab> TAB = REGISTRY.register(
			"tab",
			() -> PlatformHelper.get().newCreativeModeTab()
				.icon(() -> new ItemStack(Items.PLASMA_CAPACITOR.get()))
				.title(Component.translatable("itemGroup." + Constants.MOD_ID))
				.displayItems((context, out) -> {
					Items.TAB_ITEMS.stream().map(RegistryEntry::get).forEach(out::accept);
				})
				.build()
		);
	}

	@FunctionalInterface
	public interface EntityRendererRegister {
		<T extends Entity> void accept(EntityType<? extends T> type, final EntityRendererProvider<T> provider);
	}
}
