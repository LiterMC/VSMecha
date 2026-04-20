package com.github.litermc.vsmecha.block.control;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CapsuleHeadBlock extends Block {
	public CapsuleHeadBlock(final BlockBehaviour.Properties props) {
		super(props);
	}

	@SuppressWarnings("deprecation")
	@Override
	public InteractionResult use(
		final BlockState state,
		final Level level,
		final BlockPos pos,
		final Player player,
		final InteractionHand hand, 
		final BlockHitResult hit
	) {
		final BlockPos posBelow = pos.below();
		final BlockState stateBelow = level.getBlockState(posBelow);
		if (!(stateBelow.getBlock() instanceof CapsuleSeatBlock seatBlock)) {
			return super.use(state, level, pos, player, hand, hit);
		}
		return seatBlock.use(stateBelow, level, posBelow, player, hand, hit);
	}
}
