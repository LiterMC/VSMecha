package com.github.litermc.vsmecha.compat.computercraft;

import com.github.litermc.vsmecha.block.IPeripheralBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

public abstract class BasePeripheral<T extends BlockEntity & IPeripheralBlockEntity> implements IPeripheral {
	protected final T be;

	protected BasePeripheral(final T be) {
		this.be = be;
	}

	@Override
	public Object getTarget() {
		return this.be;
	}

	@Override
	public boolean equals(final IPeripheral other) {
		if (this == other) {
			return true;
		}
		if (other instanceof BasePeripheral<?> otherPeripheral) {
			return this.be == otherPeripheral.be;
		}
		return false;
	}
}
