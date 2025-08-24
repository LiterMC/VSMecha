package com.github.litermc.vsmecha.block.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ThermalEnergyCoreBlock extends ThermalBasedBlock {
	protected ThermalEnergyCoreBlock(final BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public ThermalEnergyCoreBlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new ThermalEnergyCoreBlockEntity(pos, state);
	}
}
