package com.github.litermc.vsmecha.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ToolBaseBlock extends BaseBlock {
	protected ToolBaseBlock(final BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public ToolBaseBlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new ToolBaseBlockEntity(pos, state);
	}
}
