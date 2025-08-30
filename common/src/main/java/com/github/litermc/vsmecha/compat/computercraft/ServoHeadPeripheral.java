package com.github.litermc.vsmecha.compat.computercraft;

import com.github.litermc.vsmecha.block.IPeripheralBlockEntity;
import com.github.litermc.vsmecha.block.joint.ServoBlockEntity;
import com.github.litermc.vsmecha.block.joint.ServoHeadBlockEntity;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipModemPeripheral;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;

import org.valkyrienskies.core.api.ships.ServerShip;

public class ServoHeadPeripheral implements IPeripheral {
	protected final ServoHeadBlockEntity be;

	public ServoHeadPeripheral(final ServoHeadBlockEntity be) {
		this.be = be;
	}

	@Override
	public String getType() {
		return "servo_head";
	}

	@Override
	public Object getTarget() {
		return this.be;
	}

	@LuaFunction
	public final boolean isAttached() {
		return this.be.isAttached();
	}

	@LuaFunction(mainThread = true)
	public final MethodResult getPeerShip() {
		final ServerShip ship = this.be.getPeerShip();
		if (ship == null) {
			return MethodResult.of();
		}
		return MethodResult.of(ship.getId(), ship.getSlug());
	}

	@LuaFunction(mainThread = true)
	public final String getPeerName() {
		final ServerLevel level = (ServerLevel) (this.be.getLevel());
		final BlockPos peerPos = this.be.getAttachingBlock();
		if (peerPos == null) {
			return null;
		}
		if (!(level.getBlockEntity(peerPos) instanceof IPeripheralBlockEntity be)) {
			return null;
		}
		if (!(be.getShipModemPeripheral() instanceof ShipModemPeripheral modem)) {
			return null;
		}
		return modem.getLocalPeripheral().getConnectedName();
	}

	@Override
	public boolean equals(final IPeripheral other) {
		if (this == other) {
			return true;
		}
		if (other instanceof ServoHeadPeripheral otherPeripheral) {
			return this.be == otherPeripheral.be;
		}
		return false;
	}
}
