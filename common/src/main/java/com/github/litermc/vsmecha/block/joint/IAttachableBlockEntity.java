package com.github.litermc.vsmecha.block.joint;

import net.minecraft.core.BlockPos;

public interface IAttachableBlockEntity extends IJointBlockEntity {
	boolean attachTo(BlockPos otherPos);

	boolean detach();

	boolean tryAttach();
}
