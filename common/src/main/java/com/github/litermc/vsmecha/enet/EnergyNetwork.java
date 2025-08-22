package com.github.litermc.vsmecha.enet;

import com.github.litermc.vsmecha.block.IMechaBlockEntity;
import com.github.litermc.vsmecha.util.ShipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

public final class EnergyNetwork {
	private final ServerLevel level;
	private final ServerShipWorldCore world;
	private final Map<BlockPos, PrioBlockPos> lastPrioBPs = new HashMap<>();
	private final Map<Long, NavigableSet<PrioBlockPos>> energyBlocks = new HashMap<>();
	private int tickAvailableEnergy = 0;
	private int tickUsedEnergy = 0;

	public EnergyNetwork(final ServerLevel level) {
		this.level = level;
		this.world = VSGameUtilsKt.getShipObjectWorld(level);
	}

	public int getTickAvailableEnergy() {
		return this.tickAvailableEnergy;
	}

	public int getTickUsedEnergy() {
		return this.tickUsedEnergy;
	}

	private void addBlock(final BlockPos pos, final IMechaBlockEntity be) {
		final PrioBlockPos lastPrioBP = this.lastPrioBPs.get(pos);
		if (lastPrioBP != null) {
			this.lastPrioBPs.remove(pos);
			this.energyBlocks.get(lastPrioBP.shipId).remove(lastPrioBP);
		}

		final ServerShip ship = ShipUtil.getServerShip(this.level, pos);
		if (ship == null) {
			return;
		}
		final Long shipId = Long.valueOf(ship.getId());
		this.energyBlocks.computeIfAbsent(shipId, (shipId0) -> new TreeSet<>())
			.add(new PrioBlockPos(be.getEnergyPriority(), pos.immutable(), shipId));
	}

	private void updatePriority(final BlockPos pos, final IMechaBlockEntity be) {
		final PrioBlockPos lastPrioBP = this.lastPrioBPs.get(pos);
		final int newPrio = be.getEnergyPriority();
		if (lastPrioBP.priority == newPrio) {
			return;
		}
		final PrioBlockPos newPrioBP = lastPrioBP.withPriority(newPrio);
		this.lastPrioBPs.put(newPrioBP.pos, newPrioBP);
		final NavigableSet<PrioBlockPos> blockSet = this.energyBlocks.get(lastPrioBP.shipId);
		blockSet.remove(lastPrioBP);
		blockSet.add(newPrioBP);
	}

	private record PrioBlockPos(int priority, BlockPos pos, Long shipId) implements Comparable<PrioBlockPos> {
		public PrioBlockPos withPriority(final int priority) {
			return new PrioBlockPos(priority, this.pos, this.shipId);
		}

		@Override
		public int compareTo(final PrioBlockPos other) {
			if (this.priority < other.priority) {
				return -1;
			}
			if (this.priority > other.priority) {
				return 1;
			}
			return this.pos.compareTo(other.pos);
		}
	}
}
