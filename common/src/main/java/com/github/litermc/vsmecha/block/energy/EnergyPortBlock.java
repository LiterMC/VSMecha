package com.github.litermc.vsmecha.block.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class EnergyPortBlock extends EnergyBasedBlock {
	public EnergyPortBlock(final BlockBehaviour.Properties props) {
		super(props);
		this.registerDefaultState(
			this.defaultBlockState()
				.setValue(BlockStateProperties.FACING, Direction.UP)
		);
	}

	@Override
	public void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BlockStateProperties.FACING);
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext ctx) {
		Direction dir = ctx.getNearestLookingDirection();
		if (!ctx.isSecondaryUseActive()) {
			dir = dir.getOpposite();
		}
		return this.defaultBlockState()
			.setValue(BlockStateProperties.FACING, dir);
	}

	@Override
	public EnergyPortBlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new EnergyPortBlockEntity(pos, state);
	}
}
