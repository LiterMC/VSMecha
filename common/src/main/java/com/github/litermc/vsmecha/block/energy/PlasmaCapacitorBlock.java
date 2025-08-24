package com.github.litermc.vsmecha.block.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PlasmaCapacitorBlock extends EnergyBasedBlock {
	public PlasmaCapacitorBlock(final BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public PlasmaCapacitorBlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new PlasmaCapacitorBlockEntity(pos, state);
	}
}
