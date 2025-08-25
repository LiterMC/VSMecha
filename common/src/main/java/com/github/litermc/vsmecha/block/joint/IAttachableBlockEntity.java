package com.github.litermc.vsmecha.block.joint;

import net.minecraft.core.BlockPos;

import org.valkyrienskies.core.api.ships.ServerShip;

public interface IAttachableBlockEntity {
	default boolean isAttached() {
		return this.getAttachingBlock() != null;
	}

	BlockPos getAttachingBlock();

	/**
	 * @return Peer ship, or {@code null} if peer ship does not present
	 */
	ServerShip getPeerShip();

	boolean canTransferEnergy();

	boolean attachTo(BlockPos otherPos);

	boolean detach();

	boolean tryAttach();
}
