package com.github.litermc.vsmecha.block.radar;

import com.github.litermc.vsmecha.block.energy.EnergyBasedBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RadarBlock extends EnergyBasedBlock {
	public RadarBlock(final BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public abstract RadarBlockEntity newBlockEntity(final BlockPos pos, final BlockState state);
}
