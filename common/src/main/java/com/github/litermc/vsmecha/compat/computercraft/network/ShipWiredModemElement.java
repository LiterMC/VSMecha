package com.github.litermc.vsmecha.compat.computercraft.network;

import com.github.litermc.vsmecha.block.control.CapsuleSeatBlockEntity;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemElement;

public final class ShipWiredModemElement extends WiredModemElement {
	private final BlockEntity be;
	private ShipModemPeripheral modem;

	public ShipWiredModemElement(final BlockEntity be) {
		this.be = be;
	}

	void setModemPeripheral(final ShipModemPeripheral modem) {
		this.modem = modem;
	}

	@Override
	protected void attachPeripheral(final String name, final IPeripheral peripheral) {
		this.modem.attachPeripheral(name, peripheral);
	}

	@Override
	protected void detachPeripheral(final String name) {
		this.modem.detachPeripheral(name);
	}

	@Override
	public Level getLevel() {
		return this.be.getLevel();
	}

	@Override
	public Vec3 getPosition() {
		return this.be.getBlockPos().getCenter();
	}
}
