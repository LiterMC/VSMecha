package com.github.litermc.vsmecha.block.joint;

import net.minecraft.core.BlockPos;

public interface IAttachableBlockEntity {
	default boolean isAttached() {
		return this.getAttachingBlock() != null;
	}

	BlockPos getAttachingBlock();

	/**
	 * This method does not need return if the block is not attached
	 *
	 * @throws RuntimeException if peer ship does not present
	 * @return peer ship's ID
	 */
	long getPeerShipId();

	boolean attachTo(BlockPos otherPos);

	boolean detach();
}
