package com.github.litermc.vsmecha.block;

import net.minecraft.core.Direction;

public interface IJointPeripheralBlockEntity extends IPeripheralBlockEntity {
	boolean canConnectPeripheralWire(Direction dir);
}
