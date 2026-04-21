package com.github.litermc.vsmecha.block.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public abstract class EnergyBasedBlock extends ThermalBasedBlock {
	protected EnergyBasedBlock(final BlockBehaviour.Properties props) {
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
		if (!(level.getBlockEntity(pos) instanceof EnergyBasedBlockEntity be)) {
			return super.use(state, level, pos, player, hand, hit);
		}
		final ItemStack stack = player.getItemInHand(hand);
		if (!(stack.getItem() instanceof NameTagItem) || !stack.hasCustomHoverName()) {
			return super.use(state, level, pos, player, hand, hit);
		}
		final String name = stack.getHoverName().getString();
		be.setName(name);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public abstract EnergyBasedBlockEntity newBlockEntity(final BlockPos pos, final BlockState state);
}
