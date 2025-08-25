package com.github.litermc.vsmecha.block.joint;

import net.minecraft.core.BlockPos;

import org.valkyrienskies.core.api.ships.ServerShip;

public interface IJointBlockEntity {
	default boolean isAttached() {
		return this.getAttachingBlock() != null;
	}

	BlockPos getAttachingBlock();

	/**
	 * @return Peer ship, or {@code null} if peer ship does not present
	 */
	ServerShip getPeerShip();

	boolean canTransferEnergy();
}
