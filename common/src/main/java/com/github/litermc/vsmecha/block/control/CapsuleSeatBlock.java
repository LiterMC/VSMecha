package com.github.litermc.vsmecha.block.control;

import com.github.litermc.vsmecha.block.energy.EnergyBasedBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public class CapsuleSeatBlock extends EnergyBasedBlock {
	public CapsuleSeatBlock(final BlockBehaviour.Properties props) {
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
	public CapsuleSeatBlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new CapsuleSeatBlockEntity(pos, state);
	}

	@Override
	public InteractionResult use(
		final BlockState state,
		final Level level,
		final BlockPos pos,
		final Player player,
		final InteractionHand hand, 
		final BlockHitResult hit
	) {
		if (!(level.getBlockEntity(pos) instanceof CapsuleSeatBlockEntity be)) {
			return InteractionResult.PASS;
		}
		return be.onUse(player, hit) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
	}

	@Override
	public void onRemove(
		final BlockState state,
		final Level level,
		final BlockPos pos,
		final BlockState newState,
		final boolean isMoving
	) {
		if (!isMoving && level.getBlockEntity(pos) instanceof CapsuleSeatBlockEntity be) {
			be.onRemove();
		}
	}
}
