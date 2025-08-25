package com.github.litermc.vsmecha.block.energy;

import com.github.litermc.vsmecha.api.HeatAPI;
import com.github.litermc.vsmecha.block.BaseBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;

public abstract class ThermalBasedBlock extends BaseBlock {
	protected ThermalBasedBlock(final BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean isMoving) {
		// TODO: maybe find a way to distribute remaining heat
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource rnd) {
		super.randomTick(state, level, pos, rnd);
		if (!(level.getBlockEntity(pos) instanceof ThermalBasedBlockEntity be)) {
			return;
		}
		if (!be.isDangerous()) {
			return;
		}
		be.onTickDangerous(rnd);
	}

	@Override
	public abstract ThermalBasedBlockEntity newBlockEntity(final BlockPos pos, final BlockState state);
}
