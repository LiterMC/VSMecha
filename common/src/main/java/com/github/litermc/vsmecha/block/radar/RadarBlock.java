package com.github.litermc.vsmecha.block.radar;

import com.github.litermc.vsmecha.block.energy.EnergyBasedBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public abstract class RadarBlock extends EnergyBasedBlock {
	public static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;

	public RadarBlock(final BlockBehaviour.Properties props) {
		super(props);
		BlockState defaultState = this.defaultBlockState()
			.setValue(ORIENTATION, FrontAndTop.NORTH_UP);
		this.registerDefaultState(defaultState);
	}

	@Override
	public void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ORIENTATION);
	}

	@Override
	public BlockState rotate(final BlockState state, final Rotation rotation) {
		return state.setValue(ORIENTATION, rotation.rotation().rotate(state.getValue(ORIENTATION)));
	}

	@Override
	public BlockState mirror(final BlockState state, final Mirror mirror) {
		return state.setValue(ORIENTATION, mirror.rotation().rotate(state.getValue(ORIENTATION)));
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext context) {
		final Direction frontDir = context.getNearestLookingDirection().getOpposite();
		final Direction topDir = frontDir == Direction.UP
			? context.getHorizontalDirection()
			: frontDir == Direction.DOWN
				? context.getHorizontalDirection().getOpposite()
				: Direction.UP;
		return this.defaultBlockState().setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(frontDir, topDir));
	}

	@Override
	public abstract RadarBlockEntity newBlockEntity(final BlockPos pos, final BlockState state);
}
