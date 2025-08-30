package com.github.litermc.vsmecha.compat.computercraft;

import com.github.litermc.vsmecha.block.energy.ThermalEnergyCoreBlockEntity;

import dan200.computercraft.api.lua.LuaFunction;

public class ThermalEnergyCorePeripheral extends EnergyBasedPeripheral<ThermalEnergyCoreBlockEntity> {
	public ThermalEnergyCorePeripheral(final ThermalEnergyCoreBlockEntity be) {
		super(be);
	}

	@Override
	public String getType() {
		return "thermal_energy_core";
	}

	@LuaFunction
	public final boolean getSafeMode() {
		return this.be.getSafeMode();
	}

	@LuaFunction
	public final void setSafeMode(final boolean safeMode) {
		this.be.setSafeMode(safeMode);
	}
}
