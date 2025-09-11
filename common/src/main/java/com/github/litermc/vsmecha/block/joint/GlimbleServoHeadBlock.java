package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.block.BaseBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class GlimbleServoHeadBlock extends BaseBlock {
	private static final double HEIGHT = 1;
	private static final EnumMap<Direction, VoxelShape> SHAPES = new EnumMap<>(Map.of(
		Direction.DOWN, Block.box(0, 16 - HEIGHT, 0, 16, 16, 16),
		Direction.UP, Block.box(0, 0, 0, 16, HEIGHT, 16),
		Direction.NORTH, Block.box(0, 0, 16 - HEIGHT, 16, 16, 16),
		Direction.SOUTH, Block.box(0, 0, 0, 16, 16, HEIGHT),
		Direction.WEST, Block.box(16 - HEIGHT, 0, 0, 16, 16, 16),
		Direction.EAST, Block.box(0, 0, 0, HEIGHT, 16, 16)
	));

	public GlimbleServoHeadBlock(final BlockBehaviour.Properties props) {
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
		final Direction dir = ctx.getClickedFace();
		return this.defaultBlockState()
			.setValue(BlockStateProperties.FACING, dir);
	}

	@Override
	public GlimbleServoHeadBlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new GlimbleServoHeadBlockEntity(pos, state);
	}

	@Override
	public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
		return SHAPES.get(state.getValue(BlockStateProperties.FACING));
	}
}
