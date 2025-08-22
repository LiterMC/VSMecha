package com.github.litermc.vsmecha.block.joint;

import net.minecraft.core.BlockPos;

public interface IAttachableBlockEntity {
	default boolean isAttached() {
		return this.getAttachingBlock() != null;
	}

	BlockPos getAttachingBlock();

	boolean attachTo(BlockPos otherPos);

	boolean detach();
}
