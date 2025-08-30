package com.github.litermc.vsmecha.api;

import com.github.litermc.vsmecha.block.energy.IThermalBlockEntity;
import com.github.litermc.vsmecha.util.ShipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.HashMap;
import java.util.Map;

public final class HeatAPI {
	private HeatAPI() {}

	private static final double ZERO_CELSIUS = 273.15;

	private static final Map<Fluid, Integer> FLUID_HEATS = new HashMap<>();
	private static final Map<Block, Integer> BLOCK_HEATS = new HashMap<>();
	static {
		// TODO: data system instead?
		FLUID_HEATS.put(Fluids.LAVA, 1300 * 10000 / 273);
		FLUID_HEATS.put(Fluids.FLOWING_LAVA, 1300 * 10000 / 273);
		BLOCK_HEATS.put(Blocks.MAGMA_BLOCK, 1300 * 10000 / 273 * 2 / 3);
		BLOCK_HEATS.put(Blocks.CAMPFIRE, 28000);
		BLOCK_HEATS.put(Blocks.FIRE, 28000);
		BLOCK_HEATS.put(Blocks.TORCH, 20000);
		BLOCK_HEATS.put(Blocks.WALL_TORCH, 20000);
		BLOCK_HEATS.put(Blocks.SOUL_CAMPFIRE, 18000);
		BLOCK_HEATS.put(Blocks.SOUL_FIRE, 18000);
		BLOCK_HEATS.put(Blocks.SOUL_TORCH, 14000);
		BLOCK_HEATS.put(Blocks.SOUL_WALL_TORCH, 14000);

		BLOCK_HEATS.put(Blocks.SNOW, 10000);
		BLOCK_HEATS.put(Blocks.SNOW_BLOCK, 8000);
		BLOCK_HEATS.put(Blocks.FROSTED_ICE, 10000);
		BLOCK_HEATS.put(Blocks.ICE, 9000);
		BLOCK_HEATS.put(Blocks.PACKED_ICE, 6000);
		BLOCK_HEATS.put(Blocks.BLUE_ICE, 1000);
	}

	public static double unitToKelvin(final int heat) {
		return heat * ZERO_CELSIUS / 10000.0;
	}

	public static double unitToCelsius(final int heat) {
		return unitToKelvin(heat) - ZERO_CELSIUS;
	}

	public static int kelvinToUnit(final double heat) {
		final double unit = Math.round(heat * 10000.0 / ZERO_CELSIUS);
		return unit >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (unit);
	}

	public static int celsiusToUnit(final double heat) {
		return kelvinToUnit(heat + ZERO_CELSIUS);
	}

	public static int getBlockHeat(final ServerLevel level, final BlockPos pos) {
		final BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof IThermalBlockEntity tbe && !be.isRemoved() && be.getLevel() == level) {
			return tbe.getHeat();
		}
		final BlockState state = level.getBlockState(pos);
		final Block block = state.getBlock();
		final Fluid fluid = block.getFluidState(state).getType();
		final Integer fheat = FLUID_HEATS.get(fluid);
		if (fheat != null) {
			return fheat;
		}
		final Integer bheat = BLOCK_HEATS.get(block);
		if (bheat != null) {
			return bheat;
		}
		if (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty()) {
			return Integer.MAX_VALUE;
		}
		return getBiomeHeat(level, pos);
	}

	public static int getBiomeHeat(final ServerLevel level, final BlockPos pos) {
		return getBiomeHeat(level, level.getBiome(ShipUtil.toWorldBlockPos(level, pos)));
	}

	public static int getBiomeHeat(final ServerLevel level, final Holder<Biome> biome) {
		final ResourceLocation location = biome.unwrapKey().orElseThrow().location();
		// TODO: read from minecraft data system
		return 11000;
	}
}
