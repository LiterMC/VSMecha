package com.github.litermc.vsmecha.attachment;

import com.github.litermc.vsmecha.block.IPeripheralBlockEntity;
import com.github.litermc.vsmecha.block.energy.IEnergyBlockEntity;
import com.github.litermc.vsmecha.block.joint.IJointBlockEntity;
import com.github.litermc.vsmecha.compat.CompatMods;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipGlobalWiredElement;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipModemPeripheral;
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

import dan200.computercraft.api.network.wired.WiredNetworkChange;
import dan200.computercraft.api.network.wired.WiredNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class ShipNetworkAttachment {
	private final Set<BlockPos> energyBlocks = new HashSet<>();
	private final Set<BlockPos> joints = new HashSet<>();
	private final Map<BlockPos, Object> peripherals = new HashMap<>();

	private boolean ticking = false;
	private long lastTickAvailableEnergy = 0;
	private long lastTickUsedEnergy = 0;
	private NavigableSet<PrioEnergyRecord> lastTickedEnergyBlocks = Collections.emptyNavigableSet();
	private Set<ShipNetworkAttachment> lastTickedNetworks = Collections.emptySet();

	private Object /*WiredNode*/ globalNode = null;

	public ShipNetworkAttachment() {}

	public static ShipNetworkAttachment get(final ServerShip ship) {
		final ShipNetworkAttachment attachment = ship.getAttachment(ShipNetworkAttachment.class);
		if (attachment != null) {
			return attachment;
		}
		final ShipNetworkAttachment newAttachment = new ShipNetworkAttachment();
		ship.saveAttachment(ShipNetworkAttachment.class, newAttachment);
		return newAttachment;
	}

	public NavigableSet<PrioEnergyRecord> getLastTickedEnergyBlocks() {
		return this.lastTickedEnergyBlocks;
	}

	private Object getGlobalNode(final ServerLevel level) {
		if (this.globalNode == null) {
			this.globalNode = new ShipGlobalWiredElement(level).getNode();
		}
		return this.globalNode;
	}

	public void addBlockEntity(final BlockEntity be) {
		final BlockPos pos = be.getBlockPos();
		if (be instanceof IEnergyBlockEntity) {
			this.energyBlocks.add(pos);
		}
		if (be instanceof IJointBlockEntity) {
			this.joints.add(pos);
		}
		if (
			CompatMods.COMPUTERCRAFT.isLoaded() &&
			be instanceof IPeripheralBlockEntity pbe &&
			pbe.getShipModemPeripheral() instanceof ShipModemPeripheral modem
		) {
			if (this.peripherals.put(pos, modem) != modem) {
				((WiredNode) (this.getGlobalNode((ServerLevel) (be.getLevel())))).connectTo(modem.getElement().getNode());
			}
		}
	}

	private void updateTickedNetworks(final Set<ShipNetworkAttachment> tickedNetworks) {
		final Set<ShipNetworkAttachment> connectedNetworks = new HashSet<>(tickedNetworks);
		final Set<ShipNetworkAttachment> disconnectedNetworks = new HashSet<>(this.lastTickedNetworks);
		connectedNetworks.removeAll(this.lastTickedNetworks);
		disconnectedNetworks.removeAll(tickedNetworks);

		if (CompatMods.COMPUTERCRAFT.isLoaded()) {
			final WiredNode selfNode = (WiredNode) (this.globalNode);
			for (final ShipNetworkAttachment other : connectedNetworks) {
				selfNode.connectTo((WiredNode) (other.globalNode));
			}
			for (final ShipNetworkAttachment other : disconnectedNetworks) {
				selfNode.disconnectFrom((WiredNode) (other.globalNode));
			}
		}

		this.lastTickedNetworks = tickedNetworks;
	}

	private boolean preTick(final ServerLevel level, final LoadedServerShip ship) {
		if (CompatMods.COMPUTERCRAFT.isLoaded()) {
			this.getGlobalNode(level);
		}
		if (this.ticking) {
			return false;
		}
		this.ticking = true;
		return true;
	}

	public void tick(final ServerLevel level, final LoadedServerShip ship) {
		if (!this.preTick(level, ship)) {
			return;
		}

		boolean hasThingToTick = false;
		final Set<Long> shipIDs = new HashSet<>();
		final List<ShipNetworkAttachment> networks = new ArrayList<>();
		shipIDs.add(ship.getId());
		networks.add(this);
		for (int i = 0; i < networks.size(); i++) {
			final ShipNetworkAttachment network = networks.get(i);
			hasThingToTick = hasThingToTick || !network.energyBlocks.isEmpty();

			final Set<ShipNetworkAttachment> networkSet = new HashSet<>();
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
				if (!(otherShip instanceof LoadedServerShip otherLoadedShip)) {
					continue;
				}
				final ShipNetworkAttachment otherNetwork = ShipNetworkAttachment.get(otherShip);
				networkSet.add(otherNetwork);
				if (!shipIDs.add(otherShip.getId())) {
					continue;
				}
				if (!otherNetwork.preTick(level, otherLoadedShip)) {
					// May happen when server just started
					return;
				}
				networks.add(otherNetwork);
			}
			network.updateTickedNetworks(networkSet);
		}

		if (!hasThingToTick) {
			return;
		}

		long tickAvailableEnergy = 0, tickUsedEnergy = 0;
		final NavigableSet<PrioEnergyRecord> sortedEnergyBlocks = new TreeSet<>();

		for (int i = 0; i < networks.size(); i++) {
			final ShipNetworkAttachment network = networks.get(i);

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

			final Iterator<BlockPos> pbeIter = network.peripherals.keySet().iterator();
			while (pbeIter.hasNext()) {
				final BlockPos pos = pbeIter.next();
				if (!(level.getBlockEntity(pos) instanceof IPeripheralBlockEntity)) {
					final ShipModemPeripheral modem = (ShipModemPeripheral) (network.peripherals.get(pos));
					pbeIter.remove();
					((WiredNode) (this.globalNode)).disconnectFrom(modem.getElement().getNode());
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
			final ShipNetworkAttachment attachment = ship.getAttachment(ShipNetworkAttachment.class);
			if (attachment == null) {
				continue;
			}
			attachment.tick(LevelUtil.getLevel(ship.getChunkClaimDimension()), ship);
		}
	}

	public static void postServerTick(final MinecraftServer server) {
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(server);
		for (final LoadedServerShip ship : world.getLoadedShips()) {
			final ShipNetworkAttachment attachment = ship.getAttachment(ShipNetworkAttachment.class);
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
