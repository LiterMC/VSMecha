package com.github.litermc.vsmecha.block.energy;

import com.github.litermc.vsmecha.block.BaseBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ThermalBasedBlock extends BaseBlock {
	protected ThermalBasedBlock(final BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource rnd) {
		if (!(level.getBlockEntity(pos) instanceof ThermalBasedBlockEntity be)) {
			return;
		}
		if (!be.isDangerous()) {
			return;
		}
		// TODO: burn surroundings
	}

	@Override
	public abstract ThermalBasedBlockEntity newBlockEntity(final BlockPos pos, final BlockState state);
}
