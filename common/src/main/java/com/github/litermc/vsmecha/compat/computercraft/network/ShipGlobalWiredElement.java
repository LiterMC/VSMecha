package com.github.litermc.vsmecha.compat.computercraft.network;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNetworkChange;
import dan200.computercraft.api.network.wired.WiredNode;

public final class ShipGlobalWiredElement implements WiredElement {
	private final WiredNode node = ComputerCraftAPI.createWiredNodeForElement(this);

	private final Level level;

	public ShipGlobalWiredElement(final Level level) {
		this.level = level;
	}

	@Override
	public WiredNode getNode() {
		return node;
	}

	@Override
	public String getSenderID() {
		return "ship_global_node";
	}

	@Override
	public Level getLevel() {
		return this.level;
	}

	@Override
	public Vec3 getPosition() {
		// Should not be used
		return Vec3.ZERO;
	}

	@Override
	public void networkChanged(final WiredNetworkChange change) {}
}
