package com.github.litermc.vsmecha.block.radar;

import com.github.litermc.vsmecha.block.energy.EnergyBasedBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class IFFBeaconBlock extends EnergyBasedBlock {
	public IFFBeaconBlock(final BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public IFFBeaconBlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new IFFBeaconBlockEntity(pos, state);
	}
}
