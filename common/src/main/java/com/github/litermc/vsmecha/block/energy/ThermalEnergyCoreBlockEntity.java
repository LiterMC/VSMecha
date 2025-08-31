package com.github.litermc.vsmecha.block.energy;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.api.HeatAPI;
import com.github.litermc.vsmecha.compat.computercraft.ThermalEnergyCorePeripheral;
import com.github.litermc.vsmecha.platform.PlatformHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;

public class ThermalEnergyCoreBlockEntity extends EnergyBasedBlockEntity implements Container {
	private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private volatile boolean safeMode = true;

	public ThermalEnergyCoreBlockEntity(final BlockEntityType<? extends ThermalEnergyCoreBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public ThermalEnergyCoreBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.THERMAL_ENERGY_CORE.get(), pos, state);
	}

	public boolean getSafeMode() {
		return this.safeMode;
	}

	public void setSafeMode(final boolean safeMode) {
		if (this.safeMode == safeMode) {
			return;
		}
		this.safeMode = safeMode;
		this.setChanged();
	}

	@Override
	public int getMaxHeatCapacity() {
		return 60000;
	}

	@Override
	public int getDangerousHeatLimit() {
		return 56000;
	}

	@Override
	public int getDefaultEnergyPriority() {
		return -1000;
	}

	@Override
	public int getMaxEnergyStorage() {
		return 20000;
	}

	@Override	
	public int getEnergyInputLimit() {
		return 0;
	}

	@Override
	public int getEnergyOutputLimit() {
		return this.getMaxEnergyStorage();
	}

	@Override
	public int getContainerSize() {
		return this.items.size();
	}

	@Override
	public boolean isEmpty() {
		for (final ItemStack stack : this.items) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(final int slot) {
		return this.items.get(slot);
	}

	@Override
	public ItemStack removeItem(final int slot, final int amount) {
		return ContainerHelper.removeItem(this.items, slot, amount);
	}

	@Override
	public ItemStack removeItemNoUpdate(final int slot) {
		return ContainerHelper.takeItem(this.items, slot);
	}

	@Override
	public void setItem(final int slot, final ItemStack stack) {
		this.items.set(slot, stack);
	}

	@Override
	public boolean stillValid(final Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public boolean canPlaceItem(final int slot, final ItemStack stack) {
		return PlatformHelper.get().getBurnTime(stack) > 0;
	}

	@Override
	public void clearContent() {
		this.items.clear();
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		Collections.fill(this.items, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(data, this.items);
		this.safeMode = data.getBoolean("SafeMode");
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		ContainerHelper.saveAllItems(data, this.items);
		data.putBoolean("SafeMode", this.safeMode);
	}

	@Override
	public boolean canPullByExternal(final Direction dir) {
		return !this.isOnShip();
	}

	@Override
	public boolean canPushByExternal(final Direction dir) {
		return false;
	}

	@Override
	protected Object createPeripheral() {
		return new ThermalEnergyCorePeripheral(this);
	}

	@Override
	public void serverTick() {
		super.serverTick();

		if (!this.isEnabled()) {
			return;
		}

		final int maxEnergy = this.getMaxEnergyStorage();
		final int maxProd = maxEnergy / 2;
		final int energy = this.getEnergyStored();
		final int cap = maxEnergy - energy;
		if (cap > 0) {
			final int generated = this.tryGenerateEnergy(Math.min(cap, maxProd));
			this.setEnergyStored(energy + Math.min(generated, cap));
		}
	}

	/**
	 * try generate energy
	 *
	 * @param needs needed energy
	 * @return generated energy, may larger than {@code needs}
	 */
	protected int tryGenerateEnergy(final int needs) {
		final ItemStack fuelStack = this.items.get(0);
		final int burnTime = PlatformHelper.get().getBurnTime(fuelStack);
		if (burnTime <= 0) {
			return 0;
		}
		final int generating = burnTime * 2;
		final int generated = Math.min(needs, generating);
		final int waste = generating - generated;
		if (waste > 0) {
			if (this.safeMode) {
				return 0;
			}
			this.transferHeat(waste / 16);
		}
		fuelStack.shrink(1);
		this.setChanged();
		return this.isDangerous() ? generated / 2 : generated;
	}
}
