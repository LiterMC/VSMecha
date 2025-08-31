package com.github.litermc.vsmecha.block.energy;

import com.github.litermc.vsmecha.block.BaseBlockEntity;
import com.github.litermc.vsmecha.block.IJointPeripheralBlockEntity;
import com.github.litermc.vsmecha.block.IPeripheralBlockEntity;
import com.github.litermc.vsmecha.compat.CompatMods;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipModemPeripheral;
import com.github.litermc.vsmecha.util.TaskUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.valkyrienskies.mod.common.VSGameUtilsKt;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemLocalPeripheral;
import dan200.computercraft.shared.platform.ComponentAccess;
import dan200.computercraft.shared.platform.PlatformHelper;

import java.util.HashMap;
import java.util.Map;

public abstract class EnergyBasedBlockEntity extends ThermalBasedBlockEntity implements IEnergyBlockEntity, IPeripheralBlockEntity {
	private volatile boolean enabled;
	private volatile int priority;
	private int energy = 0;
	private int empTicks = 0;

	int energyOutputRemaining = 0;
	int energyInputRemaining = 0;

	private Object /*IPeripheral*/ peripheral = null;
	public Object /*ShipModemPeripheral*/ modemPeripheral = null;
	private final Object /*ComponentAccess<WiredElement>*/ cableAccess = CompatMods.COMPUTERCRAFT.isLoaded() && this instanceof IJointPeripheralBlockEntity
		? PlatformHelper.get().createWiredElementAccess(this, (side) -> this.queueRefreshCables())
		: null;
	private volatile boolean refreshingCables = false;

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
		return this.peripheral;
	}

	@Override
	public final Object getOrCreatePeripheral() {
		if (this.peripheral == null) {
			this.peripheral = this.createPeripheral();
		}
		return this.peripheral;
	}

	@Override
	public final Object getShipModemPeripheral() {
		return this.modemPeripheral;
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.enabled = data.getBoolean("Enabled");
		this.priority = data.getInt("Priority");
		this.energy = data.getInt("Energy");
		this.empTicks = data.getInt("EMPTicks");
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putBoolean("Enabled", this.enabled);
		data.putInt("Priority", this.priority);
		data.putInt("Energy", this.energy);
		data.putInt("EMPTicks", this.empTicks);
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		if (!level.isClientSide && CompatMods.COMPUTERCRAFT.isLoaded() && (this instanceof IJointPeripheralBlockEntity || this.isOnShip())) {
			final ShipModemPeripheral modemPeripheral = new ShipModemPeripheral(this);
			this.modemPeripheral = modemPeripheral;
			final WiredModemLocalPeripheral localPeripheral = modemPeripheral.getLocalPeripheral();
			TaskUtil.queueTickEnd(() -> {
				localPeripheral.attach(level, this.getBlockPos().above(), Direction.DOWN);
				final Map<String, IPeripheral> peripheralMap = new HashMap<>();
				localPeripheral.extendMap(peripheralMap);
				modemPeripheral.getElement().getNode().updatePeripherals(peripheralMap);
			});
			if (this instanceof IJointPeripheralBlockEntity) {
				this.queueRefreshCables();
			}
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (CompatMods.COMPUTERCRAFT.isLoaded() && this.modemPeripheral instanceof ShipModemPeripheral modem) {
			modem.getElement().getNode().remove();
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

	private void queueRefreshCables() {
		if (this.refreshingCables) {
			return;
		}
		this.refreshingCables = true;
		TaskUtil.queueTickEnd(this::refreshCables);
	}

	private void refreshCables() {
		this.refreshingCables = false;
		if (!(this instanceof IJointPeripheralBlockEntity jbe)) {
			return;
		}
		final WiredNode node = ((ShipModemPeripheral) (this.modemPeripheral)).getElement().getNode();
		final ComponentAccess<WiredElement> cableAccess = (ComponentAccess<WiredElement>) (this.cableAccess);
		for (final Direction dir : Direction.values()) {
			final WiredElement element = cableAccess.get(dir);
			if (element == null) {
				continue;
			}
			if (jbe.canConnectPeripheralWire(dir)) {
				node.connectTo(element.getNode());
			} else {
				node.disconnectFrom(element.getNode());
			}
		}
	}
}
