package com.github.litermc.vsmecha.block.energy;

import com.github.litermc.vsmecha.block.BaseBlockEntity;
import com.github.litermc.vsmecha.enet.EnergyNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class EnergyBasedBlockEntity extends ThermalBasedBlockEntity implements IEnergyBlockEntity {
	private EnergyNetwork network = null;
	private boolean enabled = true;
	private int priority;
	private int energy = 0;

	protected EnergyBasedBlockEntity(final BlockEntityType<? extends EnergyBasedBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.priority = this.getDefaultEnergyPriority();
	}

	@Override
	public void onEnergyNetworkChanged(final EnergyNetwork network) {
		this.network = network;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(final boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		this.setChanged();
	}

	public int getDefaultEnergyPriority() {
		return 100;
	}

	@Override
	public int getEnergyPriority() {
		return this.priority;
	}

	@Override
	public void setEnergyPriority(final int priority) {
		if (this.priority == priority) {
			return;
		}
		this.priority = priority;
		this.setChanged();
		if (this.network != null) {
			this.network.updatePriority(this.getBlockPos(), this);
		}
	}

	public int getEnergyStorage() {
		return this.energy;
	}

	protected void setEnergyStorage(final int energy) {
		if (this.energy == energy) {
			return;
		}
		this.energy = energy;
		this.setChanged();
	}

	/**
	 * @return max energy can be stored in the block, should never be negative
	 */
	public abstract int getMaxEnergyStorage();

	/**
	 * @return energy max input rate in FE/t, should never be negative
	 */
	public abstract int getEnergyInputLimit();

	/**
	 * @return energy max output rate in FE/t, should never be negative
	 */
	public abstract int getEnergyOutputLimit();

	@Override
	public int tickEnergySource() {
		if (!this.isEnabled()) {
			return 0;
		}
		return Math.min(this.getEnergyOutputLimit(), this.energy);
	}

	@Override
	public void consumeEnergy(final int amount) {
		this.energy -= amount;
		this.setChanged();
	}

	@Override
	public int tickEnergyInput(final int available) {
		if (!this.isEnabled()) {
			return 0;
		}
		final int limit = this.getEnergyInputLimit();
		if (limit == 0) {
			return 0;
		}
		final int consumed = Math.min(this.getMaxEnergyStorage() - this.energy, Math.min(limit, available));
		if (consumed == 0) {
			return 0;
		}
		this.energy += consumed;
		this.setChanged();
		return consumed;
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.enabled = data.getBoolean("Enabled");
		this.priority = data.getInt("Priority");
		this.energy = data.getInt("Energy");
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putBoolean("Enabled", this.enabled);
		data.putInt("Priority", this.priority);
		data.putInt("Energy", this.energy);
	}
}
