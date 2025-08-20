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
