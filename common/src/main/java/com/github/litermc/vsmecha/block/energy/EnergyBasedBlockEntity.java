package com.github.litermc.vsmecha.block.energy;

import com.github.litermc.vsmecha.block.IPeripheralBlockEntity;
import com.github.litermc.vsmecha.compat.CompatMods;
import com.github.litermc.vsmecha.util.ShipPeripheralHolder;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.valkyrienskies.mod.common.VSGameUtilsKt;

public abstract class EnergyBasedBlockEntity extends ThermalBasedBlockEntity implements IEnergyBlockEntity, IPeripheralBlockEntity {
	private volatile boolean enabled;
	private volatile int priority;
	private volatile String name = null;
	private int energy = 0;
	private int empTicks = 0;

	protected int energyOutputRemaining = 0;
	protected int energyInputRemaining = 0;

	private final ShipPeripheralHolder peripheralHolder = new ShipPeripheralHolder(this, () -> (IPeripheral) this.createPeripheral());

	protected EnergyBasedBlockEntity(final BlockEntityType<? extends EnergyBasedBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.enabled = this.getDefaultEnabled();
		this.priority = this.getDefaultEnergyPriority();
	}

	public boolean getDefaultEnabled() {
		return true;
	}

	public boolean isEnabled() {
		return this.enabled && this.empTicks == 0;
	}

	public void setEnabled(final boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		this.setChanged();
	}

	public int getEMPTicks() {
		return this.empTicks;
	}

	public void setEMPTicks(final int empTicks) {
		if (this.empTicks == empTicks) {
			return;
		}
		this.empTicks = empTicks;
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
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		if (name != null && name.isEmpty()) {
			name = null;
		}
		if (this.name == null) {
			if (name == null) {
				return;
			}
		} else if (this.name.equals(name)) {
			return;
		}
		this.name = name;
		this.setChanged();
	}

	public int getEnergyStored() {
		return this.energy;
	}

	protected void setEnergyStored(final int energy) {
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

	protected boolean isOnShip() {
		return VSGameUtilsKt.isBlockInShipyard(this.getLevel(), this.getBlockPos());
	}

	public boolean hasDirectionalEnergyStorage() {
		return false;
	}

	public boolean canPullByExternal(final Direction dir) {
		return false;
	}

	public boolean canPushByExternal(final Direction dir) {
		return !this.isOnShip();
	}

	@Override
	public int tickEnergySource() {
		if (!this.isEnabled()) {
			return 0;
		}
		final int limit = this.getEnergyOutputLimit();
		this.energyOutputRemaining = limit;
		return Math.min(limit, this.energy);
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
		this.energyInputRemaining = limit - consumed;
		if (consumed == 0) {
			return 0;
		}
		this.energy += consumed;
		this.setChanged();
		return consumed;
	}

	protected abstract Object createPeripheral();

	protected final Object getPeripheral() {
		return this.peripheralHolder.getPeripheral();
	}

	@Override
	public final ShipPeripheralHolder getShipPeripheralHolder() {
		return this.peripheralHolder;
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.enabled = data.getBoolean("Enabled");
		this.priority = data.getInt("Priority");
		this.name = data.getString("Name");
		if (this.name.isEmpty()) {
			this.name = null;
		}
		this.energy = data.getInt("Energy");
		this.empTicks = data.getInt("EMPTicks");
		if (CompatMods.COMPUTERCRAFT.isLoaded()) {
			this.peripheralHolder.load(data);
		}
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putBoolean("Enabled", this.enabled);
		data.putInt("Priority", this.priority);
		data.putInt("Energy", this.energy);
		data.putInt("EMPTicks", this.empTicks);
		if (CompatMods.COMPUTERCRAFT.isLoaded()) {
			this.peripheralHolder.save(data);
		}
	}

	@Override
	protected void saveShared(final CompoundTag data) {
		super.saveShared(data);
		if (this.name != null) {
			data.putString("Name", this.name);
		}
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		if (!(level instanceof final ServerLevel serverLevel)) {
			return;
		}
		if (CompatMods.COMPUTERCRAFT.isLoaded()) {
			this.peripheralHolder.onSetLevel(serverLevel);
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (CompatMods.COMPUTERCRAFT.isLoaded()) {
			this.peripheralHolder.onRemove();
		}
	}

	@Override
	public void serverTick() {
		super.serverTick();
		if (this.empTicks > 0) {
			this.empTicks--;
			this.setChanged();
		}
	}
}
