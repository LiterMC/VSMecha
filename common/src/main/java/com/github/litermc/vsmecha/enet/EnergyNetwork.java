package com.github.litermc.vsmecha.enet;

import com.github.litermc.vsmecha.block.energy.IEnergyBlockEntity;
import com.github.litermc.vsmecha.util.ShipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

public final class EnergyNetwork {
	private final ServerLevel level;
	private final ServerShipWorldCore world;
	private final Map<BlockPos, PrioBlockPos> lastPrioBPs = new HashMap<>();
	private final NavigableSet<PrioBlockPos> energyBlocks = new TreeSet<>();
	private final Map<Long, Set<PrioBlockPos>> energyBlocksByShip = new HashMap<>();
	private long tickAvailableEnergy = 0;
	private long tickUsedEnergy = 0;

	public EnergyNetwork(final ServerLevel level) {
		this.level = level;
		this.world = VSGameUtilsKt.getShipObjectWorld(level);
	}

	/**
	 * @return total avaliable energy in this tick
	 */
	public long getTickAvailableEnergy() {
		return this.tickAvailableEnergy;
	}

	/**
	 * @return total energy used in this tick
	 */
	public long getTickUsedEnergy() {
		return this.tickUsedEnergy;
	}

	private void addBlock(final BlockPos pos, final IEnergyBlockEntity be) {
		this.removeBlock(pos);

		final ServerShip ship = ShipUtil.getServerShip(this.level, pos);
		if (ship == null) {
			return;
		}
		final Long shipId = Long.valueOf(ship.getId());
		final PrioBlockPos prioBP = new PrioBlockPos(be.getEnergyPriority(), pos.immutable(), shipId);

		this.lastPrioBPs.put(pos, prioBP);
		this.energyBlocks.add(prioBP);
		this.energyBlocksByShip.computeIfAbsent(shipId, (shipId0) -> new HashSet<>()).add(prioBP);
	}

	private void removeBlock(final BlockPos pos) {
		final PrioBlockPos lastPrioBP = this.lastPrioBPs.get(pos);
		if (lastPrioBP == null) {
			return;
		}
		this.lastPrioBPs.remove(pos);
		this.energyBlocks.remove(lastPrioBP);
		this.energyBlocksByShip.get(lastPrioBP.shipId).remove(lastPrioBP);
	}

	private void removeShip(final long id) {
		final Set<PrioBlockPos> blocks = this.energyBlocksByShip.remove(id);
		if (blocks == null || blocks.isEmpty()) {
			return;
		}
		for (final PrioBlockPos prioBP : blocks) {
			this.lastPrioBPs.remove(prioBP.pos);
			this.energyBlocks.remove(prioBP);
		}
	}

	public void updatePriority(final BlockPos pos, final IEnergyBlockEntity be) {
		final PrioBlockPos lastPrioBP = this.lastPrioBPs.get(pos);
		final int newPrio = be.getEnergyPriority();
		if (lastPrioBP.priority == newPrio) {
			return;
		}
		final PrioBlockPos newPrioBP = lastPrioBP.withPriority(newPrio);
		this.lastPrioBPs.put(newPrioBP.pos, newPrioBP);
		this.energyBlocks.remove(lastPrioBP);
		this.energyBlocks.add(newPrioBP);
	}

	public void merge(final EnergyNetwork other) {
		this.lastPrioBPs.putAll(other.lastPrioBPs);
		this.energyBlocksByShip.putAll(other.energyBlocksByShip);
		this.energyBlocks.addAll(other.energyBlocks);
	}

	public void tick() {
		this.tickAvailableEnergy = 0;
		this.tickUsedEnergy = 0;
		final ArrayDeque<EnergyRecord> erecs = new ArrayDeque<>();
		final ArrayList<IEnergyBlockEntity> ebes = new ArrayList<>(this.energyBlocks.size());
		{
			final Iterator<PrioBlockPos> iterator = this.energyBlocks.iterator();
			while (iterator.hasNext()) {
				final BlockPos pos = iterator.next().pos;
				if (!(this.level.getBlockEntity(pos) instanceof IEnergyBlockEntity ebe)) {
					iterator.remove();
					continue;
				}
				ebes.add(ebe);
				final int energy = ebe.tickEnergySource();
				if (energy <= 0) {
					continue;
				}
				erecs.add(new EnergyRecord(ebe, energy));
				this.tickAvailableEnergy += energy;
			}
		}
		long available = this.tickAvailableEnergy - this.tickUsedEnergy;
		for (int i = ebes.size() - 1; i >= 0 && available > 0; i--) {
			final IEnergyBlockEntity ebe = ebes.get(i);
			int used = ebe.tickEnergyInput(available < Integer.MAX_VALUE ? ((int) (available)) : Integer.MAX_VALUE);
			available -= used;
			this.tickUsedEnergy += used;
			while (used > 0) {
				final EnergyRecord rec = erecs.peekFirst();
				used -= rec.drain(used);
				if (rec.energy == 0) {
					erecs.removeFirst();
				}
			}
		}
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

	private static final class EnergyRecord {
		private final IEnergyBlockEntity be;
		private int energy;

		EnergyRecord(final IEnergyBlockEntity be, final int energy) {
			this.be = be;
			this.energy = energy;
		}

		int drain(final int requires) {
			final int drained = Math.min(this.energy, requires);
			this.energy -= drained;
			return drained;
		}
	}
}
