package com.github.litermc.vsmecha.block.energy;

import com.github.litermc.vsmecha.VSMechaRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PlasmaCapacitorBlockEntity extends EnergyBasedBlockEntity {
	private static final double EFFICIENCY = 0.999;

	public PlasmaCapacitorBlockEntity(final BlockEntityType<? extends PlasmaCapacitorBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public PlasmaCapacitorBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.PLASMA_CAPACITOR.get(), pos, state);
	}

	@Override
	public boolean getDefaultEnabled() {
		return false;
	}

	@Override
	public int getMaxHeatCapacity() {
		return 24000;
	}

	@Override
	public int getDangerousHeatLimit() {
		return 18000;
	}

	@Override
	public int getDefaultEnergyPriority() {
		return 0;
	}

	@Override
	public int getMaxEnergyStorage() {
		return 10000000;
	}

	@Override	
	public int getEnergyInputLimit() {
		return this.getMaxEnergyStorage();
	}

	@Override
	public int getEnergyOutputLimit() {
		return this.getMaxEnergyStorage();
	}

	@Override
	public void serverTick() {
		super.serverTick();

		final int energy = this.getEnergyStorage();
		if (energy <= 0) {
			return;
		}

		if (!this.isEnabled()) {
			this.setEnergyStorage(0);
			this.transferHeat(energy / 16);
			return;
		}

		final int newEnergy = (int) (energy * EFFICIENCY);
		final int heat = (energy - newEnergy) / 16;
		this.setEnergyStorage(newEnergy);
		this.transferHeat(heat);
	}
}
