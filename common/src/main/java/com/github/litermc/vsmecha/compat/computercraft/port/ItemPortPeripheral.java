package com.github.litermc.vsmecha.compat.computercraft.port;

import com.github.litermc.vsmecha.block.port.ItemPortBlockEntity;
import com.github.litermc.vsmecha.compat.computercraft.EnergyBasedPeripheral;

import dan200.computercraft.api.lua.LuaFunction;

public class ItemPortPeripheral extends EnergyBasedPeripheral<ItemPortBlockEntity> {
	public ItemPortPeripheral(final ItemPortBlockEntity be) {
		super(be);
	}

	@Override
	public String getType() {
		return "item_port";
	}

	@LuaFunction
	public final boolean getOverloadMode() {
		return this.be.getOverloadMode();
	}

	@LuaFunction
	public final void setOverloadMode(final boolean overloadMode) {
		this.be.setOverloadMode(overloadMode);
	}
}
