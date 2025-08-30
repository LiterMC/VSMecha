package com.github.litermc.vsmecha.compat.computercraft;

import com.github.litermc.vsmecha.block.control.CapsuleSeatBlockEntity;

import com.mojang.authlib.GameProfile;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;

import java.util.UUID;

public class CapsuleSeatPeripheral extends EnergyBasedPeripheral<CapsuleSeatBlockEntity> {
	public CapsuleSeatPeripheral(final CapsuleSeatBlockEntity be) {
		super(be);
	}

	@Override
	public String getType() {
		return "capsule_seat";
	}

	@LuaFunction
	public final boolean isLifeSupportEnabled() {
		return this.be.isLifeSupportEnabled();
	}

	@LuaFunction
	public final void setLifeSupportEnabled(final boolean lifeSupportEnabled) {
		this.be.setLifeSupportEnabled(lifeSupportEnabled);
	}

	@LuaFunction
	public final boolean hasCapsuleHead() {
		return this.be.hasCapsuleHead();
	}

	@LuaFunction
	public final boolean hasFullLifeSupport() {
		return this.be.hasFullLifeSupport();
	}

	@LuaFunction
	public final MethodResult getSeatedPlayer() {
		final UUID id = this.be.getPlayerUUID();
		if (id == null) {
			return MethodResult.of();
		}
		final GameProfile profile = this.be.getLevel().getServer().getProfileCache().get(id).orElse(null);
		if (profile == null) {
			return MethodResult.of();
		}
		return MethodResult.of(id.toString(), profile.getName());
	}
}
