package com.github.litermc.vsmecha.block.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class EnergyBasedBlock extends ThermalBasedBlock {
	protected EnergyBasedBlock(final BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public abstract EnergyBasedBlockEntity newBlockEntity(final BlockPos pos, final BlockState state);
}
