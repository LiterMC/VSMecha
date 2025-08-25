package com.github.litermc.vsmecha.attachment;

import com.github.litermc.vsmecha.block.energy.IEnergyBlockEntity;
import com.github.litermc.vsmecha.block.joint.IJointBlockEntity;
import com.github.litermc.vsmecha.util.LevelUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class EnergyNetworkAttachment {
	private final Set<BlockPos> energyBlocks = new HashSet<>();
	private final Set<BlockPos> joints = new HashSet<>();

	private boolean ticking = false;
	private long lastTickAvailableEnergy = 0;
	private long lastTickUsedEnergy = 0;
	private NavigableSet<PrioEnergyRecord> lastTickedEnergyBlocks = Collections.emptyNavigableSet();

	public EnergyNetworkAttachment() {}

	public static EnergyNetworkAttachment get(final ServerShip ship) {
		final EnergyNetworkAttachment attachment = ship.getAttachment(EnergyNetworkAttachment.class);
		if (attachment != null) {
			return attachment;
		}
		final EnergyNetworkAttachment newAttachment = new EnergyNetworkAttachment();
		ship.saveAttachment(EnergyNetworkAttachment.class, newAttachment);
		return newAttachment;
	}

	public NavigableSet<PrioEnergyRecord> getLastTickedEnergyBlocks() {
		return this.lastTickedEnergyBlocks;
	}

	public void addBlockEntity(final BlockEntity be) {
		if (be instanceof IEnergyBlockEntity ebe) {
			this.energyBlocks.add(be.getBlockPos());
		}
		if (be instanceof IJointBlockEntity jbe) {
			this.joints.add(be.getBlockPos());
		}
	}

	public void tick(final ServerLevel level, final LoadedServerShip ship) {
		if (this.ticking) {
			return;
		}
		this.ticking = true;

		int ebesPredictSize = 0;
		final Set<Long> shipIDs = new HashSet<>();
		final List<EnergyNetworkAttachment> networks = new ArrayList<>();
		shipIDs.add(ship.getId());
		networks.add(this);
		for (int i = 0; i < networks.size(); i++) {
			final EnergyNetworkAttachment network = networks.get(i);
			ebesPredictSize += network.energyBlocks.size();
			final Iterator<BlockPos> jointIter = network.joints.iterator();
			while (jointIter.hasNext()) {
				final BlockPos jointPos = jointIter.next();
				if (!(level.getBlockEntity(jointPos) instanceof IJointBlockEntity jbe)) {
					jointIter.remove();
					continue;
				}
				if (!jbe.canTransferEnergy()) {
					continue;
				}
				final ServerShip otherShip = jbe.getPeerShip();
				if (otherShip == null) {
					continue;
				}
				if (!shipIDs.add(otherShip.getId())) {
					continue;
				}
				final EnergyNetworkAttachment otherNetwork = EnergyNetworkAttachment.get(otherShip);
				if (otherNetwork.ticking) {
					throw new IllegalStateException("VSMecha connected energy network unexpectly ticked");
				}
				otherNetwork.ticking = true;
				networks.add(otherNetwork);
			}
		}

		if (ebesPredictSize == 0) {
			return;
		}

		long tickAvailableEnergy = 0, tickUsedEnergy = 0;
		final NavigableSet<PrioEnergyRecord> sortedEnergyBlocks = new TreeSet<>();
		for (int i = 0; i < networks.size(); i++) {
			final EnergyNetworkAttachment network = networks.get(i);
			if (network.energyBlocks.isEmpty()) {
				continue;
			}
			final Iterator<BlockPos> ebeIter = network.energyBlocks.iterator();
			while (ebeIter.hasNext()) {
				final BlockPos pos = ebeIter.next();
				if (!(level.getBlockEntity(pos) instanceof IEnergyBlockEntity ebe)) {
					ebeIter.remove();
					continue;
				}
				final int energy = ebe.tickEnergySource();
				sortedEnergyBlocks.add(new PrioEnergyRecord(ebe, pos, energy));
				if (energy > 0) {
					tickAvailableEnergy += energy;
				}
			}
		}

		if (tickAvailableEnergy > 0) {
			long available = tickAvailableEnergy;
			final Iterator<PrioEnergyRecord> providerIter = sortedEnergyBlocks.iterator();
			PrioEnergyRecord activeER = null;
			final Iterator<PrioEnergyRecord> consumerIter = sortedEnergyBlocks.descendingIterator();
			while (available > 0 && consumerIter.hasNext()) {
				final IEnergyBlockEntity ebe = consumerIter.next().be;
				System.out.println("consumer: " + ebe + " available: " + available);
				final int used = ebe.tickEnergyInput(available < Integer.MAX_VALUE ? ((int) (available)) : Integer.MAX_VALUE);
				available -= used;
				tickUsedEnergy += used;

				int draining = used;
				while (draining > 0) {
					if (activeER == null) {
						do {
							activeER = providerIter.next();
						} while (activeER.energy <= 0);
					}
					draining -= activeER.drain(draining);
					if (activeER.energy <= 0) {
						activeER = null;
					}
				}
			}
		}

		this.lastTickAvailableEnergy = tickAvailableEnergy;
		this.lastTickUsedEnergy = tickUsedEnergy;
		this.lastTickedEnergyBlocks = sortedEnergyBlocks;
	}

	public void postTick() {
		this.ticking = false;
	}

	public static void preServerTick(final MinecraftServer server) {
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(server);
		for (final LoadedServerShip ship : world.getLoadedShips()) {
			final EnergyNetworkAttachment attachment = ship.getAttachment(EnergyNetworkAttachment.class);
			if (attachment == null) {
				continue;
			}
			attachment.tick(LevelUtil.getLevel(ship.getChunkClaimDimension()), ship);
		}
	}

	public static void postServerTick(final MinecraftServer server) {
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(server);
		for (final LoadedServerShip ship : world.getLoadedShips()) {
			final EnergyNetworkAttachment attachment = ship.getAttachment(EnergyNetworkAttachment.class);
			if (attachment == null) {
				continue;
			}
			attachment.postTick();
		}
	}

	public static final class PrioEnergyRecord implements Comparable<PrioEnergyRecord> {
		public final IEnergyBlockEntity be;
		public final int priority;
		private final BlockPos pos;
		private int energy;

		private PrioEnergyRecord(final IEnergyBlockEntity be, final BlockPos pos, final int energy) {
			this.be = be;
			this.priority = be.getEnergyPriority();
			this.pos = pos;
			this.energy = energy;
		}

		int drain(final int requires) {
			final int drained = Math.min(this.energy, requires);
			this.energy -= drained;
			this.be.consumeEnergy(drained);
			return drained;
		}

		@Override
		public int hashCode() {
			return this.priority * 31 + this.pos.hashCode();
		}

		@Override
		public boolean equals(final Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof PrioEnergyRecord otherRecord)) {
				return false;
			}
			return this.priority == otherRecord.priority && this.pos.equals(otherRecord.pos);
		}

		@Override
		public int compareTo(final PrioEnergyRecord other) {
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
