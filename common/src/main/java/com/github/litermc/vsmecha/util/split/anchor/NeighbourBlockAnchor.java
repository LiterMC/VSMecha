package com.github.litermc.vsmecha.util.split.anchor;

import com.github.litermc.vsmecha.util.split.IBlockAnchor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;

/**
 * A simple block anchor implemention that consider any direct neighbour as connectable.
 */
public interface NeighbourBlockAnchor extends IBlockAnchor {
	@Override
	default void getConnectableBlocks(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final Collection<BlockPos> result
	) {
		for (final Direction dir : Direction.values()) {
			result.add(pos.relative(dir));
		}
	}

	@Override
	default boolean isBlockConnectable(
		LevelAccessor level,
		BlockPos pos,
		BlockState state,
		BlockPos targetPos,
		BlockState targetState
	) {
		return pos.distManhattan(targetPos) == 1;
	}

	@Override
	default boolean willConnectivityChange(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState oldState,
		final BlockState state
	) {
		return false;
	}
}
