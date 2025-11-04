package com.github.litermc.vsmecha.util.split;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;

public interface IBlockAnchor {
	/**
	 * Get all connectable blocks from current block.
	 * Will never be invoked if {@code BlockState.isAir()} returns {@code true}.
	 * All results should be add into the result collection.
	 *
	 * @param level  World the block is in.
	 * @param pos    Position of the block.
	 * @param state  The block state, will never be any air.
	 * @param result The result holder, must provides immutable {@link BlockPos}es.
	 */
	void getConnectableBlocks(LevelAccessor level, BlockPos pos, BlockState state, Collection<BlockPos> result);

	/**
	 * Check if the target block is connectable.
	 * It must returns {@code true} if the block pos is a result from {@link getConnectableBlocks}.
	 * It must returns {@code false} if the block pos is not a result from {@link getConnectableBlocks}.
	 * @param level       World the block is in.
	 * @param pos         Position of the block.
	 * @param state       The block state, will never be any air.
	 * @param targetPos   Position of target block.
	 * @param targetState Target block state, will never be any air.
	 */
	boolean isBlockConnectable(LevelAccessor level, BlockPos pos, BlockState state, BlockPos targetPos, BlockState targetState);

	/**
	 * Check if connectable blocks will change.
	 * Will only be invoked if both states' {@code isAir()} returns {@code false}.
	 *
	 * @param level    World the block is in.
	 * @param pos      Position of the block.
	 * @param oldState Previous block state, will never be any air.
	 * @param state    New block state, will never be any air.
	 */
	default boolean willConnectivityChange(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState oldState,
		final BlockState state
	) {
		return oldState != state;
	}
}
