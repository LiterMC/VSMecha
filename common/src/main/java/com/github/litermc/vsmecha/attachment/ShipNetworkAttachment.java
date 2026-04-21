package com.github.litermc.vsmecha.attachment;

import com.github.litermc.vsmecha.block.IPhysTickableBlockEntity;
import com.github.litermc.vsmecha.block.energy.IEnergyBlockEntity;
import com.github.litermc.vsmecha.block.joint.IJointBlockEntity;
import com.github.litermc.vsmecha.block.port.IPortBlockEntity;
import com.github.litermc.vsmecha.block.radar.IFFBeaconBlockEntity;
import com.github.litermc.vsmecha.compat.CompatMods;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipGlobalWiredElement;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipModemPeripheral;
import com.github.litermc.vsmecha.util.LevelUtil;
import com.github.litermc.vsmecha.util.ShipPeripheralHolder;
import com.github.litermc.vsmecha.util.ShipUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ShipPhysicsListener;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import dan200.computercraft.api.network.wired.WiredNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class ShipNetworkAttachment implements ShipPhysicsListener {
	private final Set<BlockPos> energyBlocks = new HashSet<>();
	private final Set<BlockPos> joints = new HashSet<>();
	private final Map<BlockPos, IPhysTickableBlockEntity> physTickers = new ConcurrentHashMap<>();
	private final Set<BlockPos> iffBeacons = new HashSet<>();
	private final Map<BlockPos, ShipPeripheralHolder> peripherals = new HashMap<>();
	private final Map<Object, Set<BlockPos>> ports = new IdentityHashMap<>();

	private boolean ticking = false;
	private BlockPos lastIFFBeacon = null;
	private long lastTickAvailableEnergy = 0;
	private long lastTickUsedEnergy = 0;
	private NavigableSet<PrioEnergyRecord> lastTickedEnergyBlocks = Collections.emptyNavigableSet();
	private List<ShipNetworkAttachment> lastTickedNetworks = Collections.emptyList();

	private Object /*WiredNode*/ globalNode = null;

	public ShipNetworkAttachment() {}

	public static ShipNetworkAttachment get(final LoadedServerShip ship) {
		return ship.getOrPutAttachment(ShipNetworkAttachment.class, ShipNetworkAttachment::new);
	}

	public Set<BlockPos> getEnergyBlocks() {
		return this.energyBlocks;
	}

	public Set<BlockPos> getJoints() {
		return this.joints;
	}

	public BlockPos getLastIFFBeacon() {
		return this.lastIFFBeacon;
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

	private Stream<BlockPos> streamConnectedPorts0(final IPortBlockEntity be) {
		final Set<BlockPos> blocks = this.ports.get(be.getPortType());
		return blocks == null ? Stream.empty() : blocks.stream();
	}

	private <B extends BlockEntity & IPortBlockEntity> Stream<BlockEntity> streamConnectedPorts1(final B be) {
		final Object portType = be.getPortType();
		final String channel = be.getPortChannel();
		final Level level = be.getLevel();
		final BlockPos pos = be.getBlockPos();
		return this.lastTickedNetworks.stream()
			.flatMap((network) -> network.streamConnectedPorts0(be))
			.filter(Predicate.not(pos::equals))
			.map(level::getBlockEntity)
			.filter(Objects::nonNull)
			.filter(Predicate.not(BlockEntity::isRemoved))
			.filter((b) ->
				b instanceof final IPortBlockEntity pb &&
				pb.getPortType() == portType &&
				pb.getPortChannel().equals(channel)
			);
	}

	public static <B extends BlockEntity & IPortBlockEntity> Stream<BlockEntity> streamAvailablePorts(final B be) {
		if (!(ShipUtil.getServerShip((ServerLevel) (be.getLevel()), be.getBlockPos()) instanceof final LoadedServerShip ship)) {
			return Stream.empty();
		}
		return get(ship).streamConnectedPorts1(be);
	}

	public void addBlockEntity(final BlockEntity be) {
		final BlockPos pos = be.getBlockPos();
		if (be instanceof IEnergyBlockEntity) {
			this.energyBlocks.add(pos);
		}
		if (be instanceof IJointBlockEntity) {
			this.joints.add(pos);
		}
		if (be instanceof final IPhysTickableBlockEntity ticker) {
			this.physTickers.put(pos, ticker);
		}
		if (be instanceof IFFBeaconBlockEntity) {
			this.iffBeacons.add(pos);
		}
		if (be instanceof final IPortBlockEntity pbe) {
			this.ports.computeIfAbsent(pbe.getPortType(), (k) -> new HashSet<>()).add(pos);
		}
	}

	public void registerPeripheral(final ShipPeripheralHolder holder) {
		final BlockEntity be = holder.getBlockEntity();
		if (this.peripherals.put(be.getBlockPos(), holder) != holder) {
			((WiredNode) this.getGlobalNode((ServerLevel) be.getLevel())).connectTo(holder.getShipModemPeripheral().getElement().getNode());
		}
	}

	public void removeBlockEntity(final BlockEntity be) {
		final BlockPos pos = be.getBlockPos();
		if (be instanceof IEnergyBlockEntity) {
			this.energyBlocks.remove(pos);
		}
		if (be instanceof IJointBlockEntity) {
			this.joints.remove(pos);
		}
		if (be instanceof IPhysTickableBlockEntity) {
			this.physTickers.remove(pos);
		}
		if (be instanceof IFFBeaconBlockEntity) {
			this.iffBeacons.remove(pos);
		}
		if (CompatMods.COMPUTERCRAFT.isLoaded()) {
			this.peripherals.remove(pos);
		}
		if (be instanceof final IPortBlockEntity pbe) {
			this.ports.get(pbe.getPortType()).remove(pos);
		}
	}

	@Override
	public void physTick(final @NotNull PhysShip ship, final @NotNull PhysLevel world) {
		final Iterator<IPhysTickableBlockEntity> iterator = this.physTickers.values().iterator();
		while (iterator.hasNext()) {
			final IPhysTickableBlockEntity ticker = iterator.next();
			if (((BlockEntity) (ticker)).isRemoved()) {
				iterator.remove();
				continue;
			}
			ticker.physicsTick(ship, world);
		}
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

		final List<ShipNetworkAttachment> networks = new ArrayList<>();
		final Set<Long> shipIDs = new HashSet<>();
		shipIDs.add(ship.getId());
		networks.add(this);
		for (int i = 0; i < networks.size(); i++) {
			final ShipNetworkAttachment network = networks.get(i);
			hasThingToTick = hasThingToTick || !network.energyBlocks.isEmpty() || !network.peripherals.isEmpty();

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
				if (!(jbe.getPeerShip() instanceof LoadedServerShip otherLoadedShip)) {
					continue;
				}
				if (!shipIDs.add(otherLoadedShip.getId())) {
					continue;
				}
				final ShipNetworkAttachment otherNetwork = ShipNetworkAttachment.get(otherLoadedShip);
				if (!otherNetwork.preTick(level, otherLoadedShip)) {
					// May happen when server just started
					continue;
				}
				networks.add(otherNetwork);
			}
		}

		if (!hasThingToTick) {
			return;
		}

		BlockPos iffBeacon = null;
		boolean multipleIFFBeacons = false;
		long tickAvailableEnergy = 0, tickUsedEnergy = 0;
		final NavigableSet<PrioEnergyRecord> sortedEnergyBlocks = new TreeSet<>();

		for (final ShipNetworkAttachment network : networks) {
			if (!multipleIFFBeacons) {
				if (network.iffBeacons.size() > 1) {
					multipleIFFBeacons = true;
					iffBeacon = null;
				} else if (network.iffBeacons.size() == 1) {
					if (iffBeacon != null) {
						multipleIFFBeacons = true;
						iffBeacon = null;
					} else {
						iffBeacon = network.iffBeacons.iterator().next();
					}
				}
			}

			final Iterator<BlockPos> ebeIter = network.energyBlocks.iterator();
			while (ebeIter.hasNext()) {
				final BlockPos pos = ebeIter.next();
				if (!(level.getBlockEntity(pos) instanceof final IEnergyBlockEntity ebe)) {
					ebeIter.remove();
					continue;
				}
				final int energy = ebe.tickEnergySource();
				sortedEnergyBlocks.add(new PrioEnergyRecord(ebe, pos, energy));
				if (energy > 0) {
					tickAvailableEnergy += energy;
				}
			}

			final Iterator<Map.Entry<BlockPos, ShipPeripheralHolder>> pbeIter = network.peripherals.entrySet().iterator();
			while (pbeIter.hasNext()) {
				final Map.Entry<BlockPos, ShipPeripheralHolder> entry = pbeIter.next();
				final BlockPos pos = entry.getKey();
				final ShipPeripheralHolder holder = entry.getValue();
				if (holder.getBlockEntity() != level.getBlockEntity(pos)) {
					pbeIter.remove();
					final ShipModemPeripheral modem = holder.getShipModemPeripheral();
					modem.getElement().getNode().remove();
				}
			}

			for (final Map.Entry<Object, Set<BlockPos>> entry : network.ports.entrySet()) {
				final Object portType = entry.getKey();
				final Iterator<BlockPos> portIter = entry.getValue().iterator();
				while (portIter.hasNext()) {
					final BlockPos pos = portIter.next();
					if (!(level.getBlockEntity(pos) instanceof final IPortBlockEntity pbe) || pbe.getPortType() != portType) {
						portIter.remove();
						continue;
					}
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

		final List<ShipNetworkAttachment> lastTickedNetworks = List.copyOf(networks);

		for (final ShipNetworkAttachment network : networks) {
			network.lastIFFBeacon = iffBeacon;
			network.lastTickAvailableEnergy = tickAvailableEnergy;
			network.lastTickUsedEnergy = tickUsedEnergy;
			network.lastTickedEnergyBlocks = sortedEnergyBlocks;
			network.lastTickedNetworks = lastTickedNetworks;
		}
	}

	public void postTick() {
		this.ticking = false;
	}

	public static void preServerTick(final MinecraftServer server) {
		final VsiServerShipWorld world = VSGameUtilsKt.getShipObjectWorld(server);
		for (final LoadedServerShip ship : world.getLoadedShips()) {
			final ShipNetworkAttachment attachment = ship.getAttachment(ShipNetworkAttachment.class);
			if (attachment == null) {
				continue;
			}
			attachment.tick(LevelUtil.getLevel(ship.getChunkClaimDimension()), ship);
		}
	}

	public static void postServerTick(final MinecraftServer server) {
		final VsiServerShipWorld world = VSGameUtilsKt.getShipObjectWorld(server);
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
