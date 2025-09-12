package com.github.litermc.vsmecha.compat.computercraft;

import com.github.litermc.vsmecha.block.IPeripheralBlockEntity;
import com.github.litermc.vsmecha.block.joint.GimbalServoBlockEntity;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipModemPeripheral;
import com.github.litermc.vsmecha.util.pid.PID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaTable;
import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.api.lua.MethodResult;

import org.valkyrienskies.core.api.ships.ServerShip;

public class GimbalServoPeripheral extends EnergyBasedPeripheral<GimbalServoBlockEntity> {
	public GimbalServoPeripheral(final GimbalServoBlockEntity be) {
		super(be);
	}

	@Override
	public String getType() {
		return "gimbal_servo";
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
	public final MethodResult getCurrentRotation() {
		final Quaterniondc rotation = this.be.getCurrentRotation();
		return MethodResult.of(rotation.x(), rotation.y(), rotation.z(), rotation.w());
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
	public final int getPositionLoopScale() {
		return this.be.getPositionLoopScale();
	}

	@LuaFunction
	public final void setPositionLoopScale(final int positionLoopScale) {
		this.be.setPositionLoopScale(positionLoopScale);
	}

	@LuaFunction
	public final MethodResult getTargetRotation() {
		final Quaterniondc rotation = this.be.getTargetRotation();
		return MethodResult.of(rotation.x(), rotation.y(), rotation.z(), rotation.w());
	}

	@LuaFunction
	public final void setTargetRotation(final IArguments args) throws LuaException {
		final double x, y, z, w;
		if (args.get(0) instanceof Number) {
			x = args.getFiniteDouble(0);
			y = args.getFiniteDouble(1);
			z = args.getFiniteDouble(2);
			w = args.getFiniteDouble(3);
		} else {
			final LuaTable<?, ?> quat = args.getTableUnsafe(0);
			x = getFiniteDouble(quat, "x");
			y = getFiniteDouble(quat, "y");
			z = getFiniteDouble(quat, "z");
			w = getFiniteDouble(quat, "w");
		}
		this.be.setTargetRotation(new Quaterniond(x, y, z, w));
	}

	@LuaFunction
	public final MethodResult getLastOmega() {
		final Vector3dc omega = this.be.getLastOmega();
		return MethodResult.of(omega.x(), omega.y(), omega.z());
	}

	@LuaFunction
	public final MethodResult getLastTorque() {
		final Vector3dc torque = this.be.getLastTorque();
		return MethodResult.of(torque.x(), torque.y(), torque.z());
	}

	@LuaFunction
	public final double getPitchTargetVelocity() {
		return this.be.getPitchTargetVelocity();
	}

	@LuaFunction
	public final void setPitchTargetVelocity(final double velocity) {
		this.be.setPitchTargetVelocity(velocity);
	}

	@LuaFunction
	public final double getPitchMaxRotationSpeed() {
		return this.be.getPitchMaxRotationSpeed();
	}

	@LuaFunction
	public final void setPitchMaxRotationSpeed(final double speed) {
		this.be.setPitchMaxRotationSpeed(speed);
	}

	@LuaFunction
	public final MethodResult getPitchPosPID() {
		final PID pid = this.be.getPitchPosPID();
		return MethodResult.of(pid.getKp(), pid.getKi(), pid.getKd());
	}

	@LuaFunction
	public final void setPitchPosPID(final double p, final double i, final double d) {
		this.be.setPitchPosPID(p, i, d);
	}

	@LuaFunction
	public final MethodResult getPitchVelPID() {
		final PID pid = this.be.getPitchVelPID();
		return MethodResult.of(pid.getKp(), pid.getKi(), pid.getKd());
	}

	@LuaFunction
	public final void setPitchVelPID(final double p, final double i, final double d) {
		this.be.setPitchVelPID(p, i, d);
	}

	@LuaFunction
	public final double getPitchFeedForwardForce() {
		return this.be.getPitchFeedForwardForce();
	}

	@LuaFunction
	public final void setPitchFeedForwardForce(final double force) {
		this.be.setPitchFeedForwardForce(force);
	}

	@LuaFunction
	public final double getYawTargetVelocity() {
		return this.be.getYawTargetVelocity();
	}

	@LuaFunction
	public final void setYawTargetVelocity(final double velocity) {
		this.be.setYawTargetVelocity(velocity);
	}

	@LuaFunction
	public final double getYawMaxRotationSpeed() {
		return this.be.getYawMaxRotationSpeed();
	}

	@LuaFunction
	public final void setYawMaxRotationSpeed(final double speed) {
		this.be.setYawMaxRotationSpeed(speed);
	}

	@LuaFunction
	public final MethodResult getYawPosPID() {
		final PID pid = this.be.getYawPosPID();
		return MethodResult.of(pid.getKp(), pid.getKi(), pid.getKd());
	}

	@LuaFunction
	public final void setYawPosPID(final double p, final double i, final double d) {
		this.be.setYawPosPID(p, i, d);
	}

	@LuaFunction
	public final MethodResult getYawVelPID() {
		final PID pid = this.be.getYawVelPID();
		return MethodResult.of(pid.getKp(), pid.getKi(), pid.getKd());
	}

	@LuaFunction
	public final void setYawVelPID(final double p, final double i, final double d) {
		this.be.setYawVelPID(p, i, d);
	}

	@LuaFunction
	public final double getYawFeedForwardForce() {
		return this.be.getYawFeedForwardForce();
	}

	@LuaFunction
	public final void setYawFeedForwardForce(final double force) {
		this.be.setYawFeedForwardForce(force);
	}

	@LuaFunction
	public final double getRollTargetVelocity() {
		return this.be.getRollTargetVelocity();
	}

	@LuaFunction
	public final void setRollTargetVelocity(final double velocity) {
		this.be.setRollTargetVelocity(velocity);
	}

	@LuaFunction
	public final double getRollMaxRotationSpeed() {
		return this.be.getRollMaxRotationSpeed();
	}

	@LuaFunction
	public final void setRollMaxRotationSpeed(final double speed) {
		this.be.setRollMaxRotationSpeed(speed);
	}

	@LuaFunction
	public final MethodResult getRollPosPID() {
		final PID pid = this.be.getRollPosPID();
		return MethodResult.of(pid.getKp(), pid.getKi(), pid.getKd());
	}

	@LuaFunction
	public final void setRollPosPID(final double p, final double i, final double d) {
		this.be.setRollPosPID(p, i, d);
	}

	@LuaFunction
	public final MethodResult getRollVelPID() {
		final PID pid = this.be.getRollVelPID();
		return MethodResult.of(pid.getKp(), pid.getKi(), pid.getKd());
	}

	@LuaFunction
	public final void setRollVelPID(final double p, final double i, final double d) {
		this.be.setRollVelPID(p, i, d);
	}

	@LuaFunction
	public final double getRollFeedForwardForce() {
		return this.be.getRollFeedForwardForce();
	}

	@LuaFunction
	public final void setRollFeedForwardForce(final double force) {
		this.be.setRollFeedForwardForce(force);
	}

	private static double getFiniteDouble(final LuaTable<?, ?> table, final String key) throws LuaException {
		final Object value = table.get(key);
		if (!(value instanceof final Number num)) {
			throw LuaValues.badField(key, "number", LuaValues.getType(value));
		}
		final double n = num.doubleValue();
		if (!Double.isFinite(n)) {
			throw LuaValues.badField(key, "number", LuaValues.getNumericType(n));
		}
		return n;
	}
}
