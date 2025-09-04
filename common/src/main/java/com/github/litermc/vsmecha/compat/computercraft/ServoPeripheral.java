package com.github.litermc.vsmecha.compat.computercraft;

import com.github.litermc.vsmecha.block.IPeripheralBlockEntity;
import com.github.litermc.vsmecha.block.joint.ServoBlockEntity;
import com.github.litermc.vsmecha.block.joint.ServoHeadBlockEntity;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipModemPeripheral;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;

import org.valkyrienskies.core.api.ships.ServerShip;

public class ServoPeripheral extends EnergyBasedPeripheral<ServoBlockEntity> {
	public ServoPeripheral(final ServoBlockEntity be) {
		super(be);
	}

	@Override
	public String getType() {
		return "servo";
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

	@LuaFunction
	public final boolean getAutoAttach() {
		return this.be.getAutoAttach();
	}

	@LuaFunction
	public final void setAutoAttach(final boolean autoAttach) {
		this.be.setAutoAttach(autoAttach);
	}

	@LuaFunction(mainThread = true)
	public final boolean attach() {
		return this.be.tryAttach();
	}

	@LuaFunction(mainThread = true)
	public final boolean detach() {
		return this.be.detach();
	}

	@LuaFunction
	public final boolean isWorking() {
		return this.be.isWorking();
	}

	@LuaFunction
	public final double getCurrentAngle() {
		return this.be.getCurrentAngle();
	}

	@LuaFunction
	public final boolean getPositionMode() {
		return this.be.getPositionMode();
	}

	@LuaFunction
	public final void setPositionMode(final boolean positionMode) {
		this.be.setPositionMode(positionMode);
	}

	@LuaFunction
	public final double getTargetAngle() {
		return this.be.getTargetAngle();
	}

	@LuaFunction
	public final void setTargetAngle(final double angle) {
		this.be.setTargetAngle(angle);
	}

	@LuaFunction
	public final double getTargetVelocity() {
		return this.be.getTargetVelocity();
	}

	@LuaFunction
	public final void setTargetVelocity(final double velocity) {
		this.be.setTargetVelocity(velocity);
	}

	@LuaFunction
	public final double getMaxRotationSpeed() {
		return this.be.getMaxRotationSpeed();
	}

	@LuaFunction
	public final void setMaxRotationSpeed(final double speed) {
		this.be.setMaxRotationSpeed(speed);
	}

	@LuaFunction
	public final double getLastTorque() {
		return this.be.getLastTorque();
	}

	@LuaFunction
	public final MethodResult getPosPID() {
		final ServoBlockEntity.PID pid = this.be.getPosPID();
		return MethodResult.of(pid.getKp(), pid.getKi(), pid.getKd());
	}

	@LuaFunction
	public final void setPosPID(final double p, final double i, final double d) {
		this.be.setPosPID(p, i, d);
	}

	@LuaFunction
	public final MethodResult getVelPID() {
		final ServoBlockEntity.PID pid = this.be.getVelPID();
		return MethodResult.of(pid.getKp(), pid.getKi(), pid.getKd());
	}

	@LuaFunction
	public final void setVelPID(final double p, final double i, final double d) {
		this.be.setVelPID(p, i, d);
	}

	@LuaFunction
	public final double getFeedForwardForce() {
		return this.be.getFeedForwardForce();
	}

	@LuaFunction
	public final void setFeedForwardForce(final double force) {
		this.be.setFeedForwardForce(force);
	}
}
