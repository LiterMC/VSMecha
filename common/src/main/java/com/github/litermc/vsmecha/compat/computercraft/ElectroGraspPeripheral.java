package com.github.litermc.vsmecha.compat.computercraft;

import com.github.litermc.vsmecha.block.joint.ElectroGraspBlockEntity;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;

import org.valkyrienskies.core.api.ships.ServerShip;

public class ElectroGraspPeripheral extends EnergyBasedPeripheral<ElectroGraspBlockEntity> {
	public ElectroGraspPeripheral(final ElectroGraspBlockEntity be) {
		super(be);
	}

	@Override
	public String getType() {
		return "electro_grasp";
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
	public final boolean attach() {
		return this.be.tryAttach();
	}

	@LuaFunction(mainThread = true)
	public final boolean detach() {
		return this.be.detach();
	}
}
