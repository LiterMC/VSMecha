package com.github.litermc.vsmecha.compat.computercraft;

import com.github.litermc.vsmecha.block.joint.ServoBlockEntity;

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
	public final double getMaxRotateSpeed() {
		return this.be.getMaxRotateSpeed();
	}

	@LuaFunction
	public final boolean isAttached() {
		return this.be.isAttached();
	}

	@LuaFunction
	public final MethodResult getPeerShip() {
		final ServerShip ship = this.be.getPeerShip();
		if (ship == null) {
			return MethodResult.of();
		}
		return MethodResult.of(ship.getId(), ship.getSlug());
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
	public final double getLastWorkingAngle() {
		return this.be.getLastWorkingAngle();
	}

	@LuaFunction
	public final double getTargetAngle() {
		return this.be.getTargetAngle();
	}

	@LuaFunction
	public final void setTargetAngle(final double angle) {
		this.be.setTargetAngle(angle);
	}
}
