package com.github.litermc.vsmecha.compat.computercraft.port;

import com.github.litermc.vsmecha.block.port.EnergyPortBlockEntity;
import com.github.litermc.vsmecha.compat.computercraft.EnergyBasedPeripheral;

import dan200.computercraft.api.lua.LuaFunction;

public class EnergyPortPeripheral extends EnergyBasedPeripheral<EnergyPortBlockEntity> {
	public EnergyPortPeripheral(final EnergyPortBlockEntity be) {
		super(be);
	}

	@Override
	public String getType() {
		return "energy_port";
	}

	@LuaFunction
	public final boolean getOverloadMode() {
		return this.be.getOverloadMode();
	}

	@LuaFunction
	public final void setOverloadMode(final boolean overloadMode) {
		this.be.setOverloadMode(overloadMode);
	}

	@LuaFunction
	public final int getLastTransferred() {
		return this.be.getLastTransferred();
	}

	@LuaFunction
	public final double getTransferLoad() {
		return this.be.getTransferLoad();
	}
}
