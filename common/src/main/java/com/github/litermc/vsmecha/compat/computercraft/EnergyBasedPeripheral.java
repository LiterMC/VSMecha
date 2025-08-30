package com.github.litermc.vsmecha.compat.computercraft;

import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.Set;

public abstract class EnergyBasedPeripheral<T extends EnergyBasedBlockEntity> implements IPeripheral {
	private static final Set<String> ADDTIONAL_TYPES = Set.of("energy_storage");

	protected final T be;

	protected EnergyBasedPeripheral(final T be) {
		this.be = be;
	}

	@Override
	public Object getTarget() {
		return this.be;
	}

	@Override
	public Set<String> getAdditionalTypes() {
		return ADDTIONAL_TYPES;
	}

	@LuaFunction
	public final boolean isEnabled() {
		return this.be.isEnabled();
	}

	@LuaFunction
	public final void setEnabled(final boolean enabled) {
		this.be.setEnabled(enabled);
	}

	@LuaFunction
	public final int getRecoverTicks() {
		return this.be.getEMPTicks();
	}

	@LuaFunction
	public final int getDefaultEnergyPriority() {
		return this.be.getDefaultEnergyPriority();
	}

	@LuaFunction
	public final int getEnergyPriority() {
		return this.be.getEnergyPriority();
	}

	@LuaFunction
	public final void setEnergyPriority(final int priority) {
		this.be.setEnergyPriority(priority);
	}

	@LuaFunction(mainThread = true)
	public final int getEnergy() {
		return this.be.getEnergyStored();
	}

	@LuaFunction(mainThread = true)
	public final int getEnergyCapacity() {
		return this.be.getMaxEnergyStorage();
	}

	@LuaFunction
	public final int getEnergyInputLimit() {
		return this.be.getEnergyInputLimit();
	}

	@LuaFunction
	public final int getEnergyOutputLimit() {
		return this.be.getEnergyOutputLimit();
	}

	@LuaFunction
	public final int getHeat() {
		return this.be.getLastTickHeat();
	}

	@LuaFunction
	public final int getMaxHeatCapacity() {
		return this.be.getMaxHeatCapacity();
	}

	@LuaFunction
	public final int getDangerousHeatLimit() {
		return this.be.getDangerousHeatLimit();
	}

	@Override
	public boolean equals(final IPeripheral other) {
		if (this == other) {
			return true;
		}
		if (other instanceof EnergyBasedPeripheral otherPeripheral) {
			return this.be == otherPeripheral.be;
		}
		return false;
	}

	public static final class Instance extends EnergyBasedPeripheral<EnergyBasedBlockEntity> {
		private final String type;

		public Instance(final String type, final EnergyBasedBlockEntity be) {
			super(be);
			this.type = type;
		}

		@Override
		public String getType() {
			return this.type;
		}
	}
}
