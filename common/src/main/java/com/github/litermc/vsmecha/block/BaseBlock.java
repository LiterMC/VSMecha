package com.github.litermc.vsmecha.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BaseBlock extends Block implements EntityBlock {
	protected BaseBlock(final BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public void neighborChanged(
		final BlockState state,
		final Level level,
		final BlockPos pos,
		final Block neighbor,
		final BlockPos neighborPos,
		final boolean moving
	) {
		super.neighborChanged(state, level, pos, neighbor, neighborPos, moving);
		if (level.getBlockEntity(pos) instanceof BaseBlockEntity be) {
			be.neighborChanged(neighbor, neighborPos, moving);
		}
	}

	@Override
	public void onRemove(
		final BlockState state,
		final Level level,
		final BlockPos pos,
		final BlockState newState,
		final boolean isMoving
	) {
		if (level.getBlockEntity(pos) instanceof BaseBlockEntity be) {
			be.beforeRemove();
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public abstract BaseBlockEntity newBlockEntity(final BlockPos pos, final BlockState state);

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
		return level.isClientSide
			? (level2, pos, state2, entity) -> {
				if (entity != null) {
					((BaseBlockEntity) (entity)).clientTick();
				}
			}
			: (level2, pos, state2, entity) -> {
				if (entity != null) {
					((BaseBlockEntity) (entity)).serverTick();
				}
			};
	}
}
