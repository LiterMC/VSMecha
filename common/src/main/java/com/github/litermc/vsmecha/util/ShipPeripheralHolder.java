package com.github.litermc.vsmecha.util;

import com.github.litermc.vsmecha.attachment.ShipNetworkAttachment;
import com.github.litermc.vsmecha.block.IJointPeripheralBlockEntity;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipModemPeripheral;
import com.github.litermc.vtil.util.TaskUtil;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemLocalPeripheral;
import dan200.computercraft.shared.platform.ComponentAccess;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ShipPeripheralHolder {
	private final BlockEntity be;
	private final Supplier<Object> peripheralSupplier;
	private Object /*IPeripheral*/ peripheral = null;
	private CompoundTag modemData = null;
	private ShipModemPeripheral modemPeripheral = null;
	private final ComponentAccess<WiredElement> cableAccess;
	private volatile boolean refreshingCables = false;

	public ShipPeripheralHolder(final BlockEntity be, final Supplier<Object> peripheralSupplier) {
		this.be = be;
		this.peripheralSupplier = peripheralSupplier;
		this.cableAccess = be instanceof IJointPeripheralBlockEntity
			? PlatformHelper.get().createWiredElementAccess(be, (side) -> this.queueRefreshCables())
			: null;
	}

	public final BlockEntity getBlockEntity() {
		return this.be;
	}

	public final IPeripheral getPeripheral() {
		return (IPeripheral) this.peripheral;
	}

	public IPeripheral getOrCreatePeripheral() {
		if (this.peripheral == null) {
			this.peripheral = this.peripheralSupplier.get();
		}
		return (IPeripheral) this.peripheral;
	}

	public final ShipModemPeripheral getShipModemPeripheral() {
		return this.modemPeripheral;
	}

	public void onSetLevel(final ServerLevel level) {
		final BlockPos pos = this.be.getBlockPos();
		final ServerShip ship = ShipUtil.getServerShip(level, pos);
		final boolean isJoint = this.be instanceof IJointPeripheralBlockEntity;
		if (ship == null && !isJoint) {
			return;
		}
		final ShipModemPeripheral modemPeripheral = new ShipModemPeripheral(this.be);
		this.modemPeripheral = modemPeripheral;
		final WiredModemLocalPeripheral localPeripheral = modemPeripheral.getLocalPeripheral();
		if (this.modemData != null) {
			localPeripheral.read(this.modemData, "");
			this.modemData = null;
		}
		TaskUtil.queueTickEnd(() -> {
			localPeripheral.attach(level, pos.above(), Direction.DOWN);
			final Map<String, IPeripheral> peripheralMap = new HashMap<>();
			localPeripheral.extendMap(peripheralMap);
			modemPeripheral.getElement().getNode().updatePeripherals(peripheralMap);
		});
		if (isJoint) {
			this.queueRefreshCables();
		}
		if (ship instanceof final LoadedServerShip loadedShip) {
			ShipNetworkAttachment.get(loadedShip).registerPeripheral(this);
		}
	}

	public void onRemove() {
		if (this.modemPeripheral != null) {
			final CompoundTag modemData = new CompoundTag();
			this.modemPeripheral.getLocalPeripheral().write(modemData, "");
			this.modemData = modemData;
			this.modemPeripheral.getElement().getNode().remove();
		}
	}

	public void load(final CompoundTag data) {
		this.modemData = data.getCompound("ModemData");
	}

	public void save(final CompoundTag data) {
		if (this.modemPeripheral != null) {
			final CompoundTag modemData = new CompoundTag();
			this.modemPeripheral.getLocalPeripheral().write(modemData, "");
			data.put("ModemData", modemData);
		} else if (this.modemData != null) {
			data.put("ModemData", this.modemData);
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
		if (!(this.be instanceof final IJointPeripheralBlockEntity jbe)) {
			return;
		}
		final WiredNode node = this.modemPeripheral.getElement().getNode();
		for (final Direction dir : Direction.values()) {
			final WiredElement element = this.cableAccess.get(dir);
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
