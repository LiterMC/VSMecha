package com.github.litermc.vsmecha.compat.computercraft.network;

import com.github.litermc.vsmecha.block.DummyBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemLocalPeripheral;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemPeripheral;
import dan200.computercraft.shared.platform.PlatformHelper;

public final class ShipModemPeripheral extends WiredModemPeripheral {
	private final BlockEntity be;
	private final ShipWiredModemElement element;
	private final WiredModemLocalPeripheral local;

	private ShipModemPeripheral(final BlockEntity be, final ShipWiredModemElement element, final WiredModemLocalPeripheral local) {
		super(new ModemState(), element, local, null);
		this.be = be;
		this.element = element;
		this.local = local;
		element.setModemPeripheral(this);
	}

	public ShipModemPeripheral(final BlockEntity be) {
		this(be, new ShipWiredModemElement(be), createModemLocalPeripheral(be));
	}

	public ShipWiredModemElement getElement() {
		return this.element;
	}

	public WiredModemLocalPeripheral getLocalPeripheral() {
		return this.local;
	}

	@Override
	public String getSenderID() {
		return "ship_modem";
	}

	@Override
	public Vec3 getPosition() {
		return this.be.getBlockPos().getCenter();
	}

	public static WiredModemLocalPeripheral createModemLocalPeripheral(final BlockEntity be) {
		return new WiredModemLocalPeripheral(
			PlatformHelper.get().createPeripheralAccess(
				new DummyBlockEntity(be.getLevel(), be.getBlockPos().above()),
				(side) -> {}
			)
		);
	}
}
