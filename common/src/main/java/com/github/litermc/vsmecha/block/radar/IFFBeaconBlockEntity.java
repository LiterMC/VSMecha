package com.github.litermc.vsmecha.block.radar;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.attachment.ShipNetworkAttachment;
import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.util.ShipUtil;
import dan200.computercraft.api.peripheral.IPeripheral;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class IFFBeaconBlockEntity extends EnergyBasedBlockEntity {
	private static final int MAX_RANGE = 160;

	private Mode mode = Mode.BROADCAST;
	private volatile HostileMode hostileMode = HostileMode.NONE;
	private final LongOpenHashSet knownAllies = new LongOpenHashSet();
	private final Set<UUID> whitelist = new HashSet<>();
	private final LongOpenHashSet hostileShips = new LongOpenHashSet();
	private final Set<UUID> hostiles = new HashSet<>();

	private UUID owner = null;

	private volatile boolean working = false;
	private int syncTicker = 0;

	private final Map<Long, Identity> identityCache = new HashMap<>();

	protected IFFBeaconBlockEntity(final BlockEntityType<? extends IFFBeaconBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public IFFBeaconBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.IFF_BEACON.get(), pos, state);
	}

	public Mode getMode() {
		return this.mode;
	}

	public void setMode(final Mode mode) {
		if (this.mode == mode) {
			return;
		}
		this.mode = mode;
		this.setChanged();
	}

	public UUID getOwner() {
		return this.owner;
	}

	public boolean isWorking() {
		return this.working;
	}

	public Set<UUID> getWhitelist() {
		return this.whitelist;
	}

	public void addToWhitelist(final UUID owner) {
		this.hostiles.remove(owner);
		this.whitelist.add(owner);
	}

	public void removeFromWhitelist(final UUID owner) {
		this.whitelist.remove(owner);
	}

	public void markHostileShip(final long shipId) {
		this.knownAllies.remove(shipId);
		this.hostileShips.add(shipId);
	}

	public void unmarkHostileShip(final long shipId) {
		this.hostileShips.remove(shipId);
	}

	public Set<UUID> getHostiles() {
		return this.hostiles;
	}

	public void markHostile(final UUID owner) {
		this.whitelist.remove(owner);
		this.hostiles.add(owner);
	}

	public void unmarkHostile(final UUID owner) {
		this.hostiles.remove(owner);
	}

	@Override
	public int getMaxHeatCapacity() {
		return 18000;
	}

	@Override
	public int getDangerousHeatLimit() {
		return 12000;
	}

	@Override
	public int getDefaultEnergyPriority() {
		return 900;
	}

	@Override
	public int getMaxEnergyStorage() {
		return 1000;
	}

	@Override
	public int getEnergyInputLimit() {
		return this.getMaxEnergyStorage();
	}

	@Override
	public int getEnergyOutputLimit() {
		return 0;
	}

	@Override
	public Object createPeripheral() {
		// TODO
		return null;
	}

	/**
	 * @return whether the IFF beacon should reply information
	 */
	public boolean onRequest(final long shipId, final UUID owner, final IFFBeaconBlockEntity requester) {
		final boolean isAlly = this.checkIsAlly(owner);
		this.onShipDetected(shipId, owner, requester, isAlly);
		if (this.mode.replyUnknown) {
			return true;
		}
		if (this.mode.shouldReply(isAlly)) {
			return true;
		}
		return false;
	}

	protected void onShipDetected(final long shipId, final UUID owner, final IFFBeaconBlockEntity be, final boolean isAlly) {
		final HostileMode mode = this.hostileMode;
		if (isAlly && (!mode.markAlly || this.owner.equals(owner))) {
			this.knownAllies.add(shipId);
			if (mode.shareWithAlly) {
				this.knownAllies.addAll(be.knownAllies);
				this.hostileShips.addAll(be.hostileShips);
				this.hostiles.addAll(be.hostiles);
			}
		} else if (mode.markNonAlly) {
			this.hostileShips.add(shipId);
			this.hostiles.add(owner);
		}
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.mode = Mode.values()[data.getByte("Mode")];
	}

	@Override
	public void saveAdditional(final CompoundTag data) {
		super.load(data);
		data.putByte("Mode", (byte) (this.mode.ordinal()));
	}

	@Override
	public void serverTick() {
		super.serverTick();

		if (!this.isEnabled() || this.owner == null) {
			this.working = false;
			return;
		}

		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos pos = this.getBlockPos();
		if (!(ShipUtil.getServerShip(level, pos) instanceof final LoadedServerShip ship)) {
			this.working = false;
			return;
		}
		// no need .equals here because getBlockPos returns a final field
		if (pos != ShipNetworkAttachment.get(ship).getLastIFFBeacon()) {
			this.working = false;
			return;
		}
		this.working = true;

		if (this.mode.shouldAsk(true)) {
			if (this.syncTicker <= 0) {
				this.syncTicker = 20;
				this.syncKnownAllies(ship);
			} else {
				this.syncTicker--;
			}
		}
	}

	protected boolean checkIsAlly(final UUID owner) {
		if (owner.equals(this.owner)) {
			return true;
		}
		// TODO: check teams
		return false;
	}

	protected void syncKnownAllies(final LoadedServerShip ship) {
		final long shipId = ship.getId();
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final VsiServerShipWorld world = VSGameUtilsKt.getShipObjectWorld(level);
		for (final long targetShipId : this.knownAllies) {
			final LoadedServerShip targetShip = world.getLoadedShips().getById(targetShipId);
			if (targetShip == null) {
				continue;
			}
			final BlockPos targetIFFPos = ShipNetworkAttachment.get(targetShip).getLastIFFBeacon();
			if (targetIFFPos == null || !(level.getBlockEntity(targetIFFPos) instanceof final IFFBeaconBlockEntity be)) {
				continue;
			}
			if (!be.onRequest(shipId, this.owner, this)) {
				continue;
			}
			final UUID owner = be.getOwner();
			this.onShipDetected(targetShipId, owner, be, this.checkIsAlly(owner));
		}
	}

	public static final class Identity {
		private final UUID owner;
		private final boolean ally;
		private boolean hostile = false;
		private Vec3 lastPosition = null;

		public Identity(final UUID owner, final boolean ally) {
			this.owner = owner;
			this.ally = ally;
		}

		public boolean isUnknown() {
			return this.owner == null;
		}

		public boolean isAlly() {
			return this.ally && !this.hostile;
		}

		public boolean isNetural() {
			return !this.ally && !this.hostile;
		}

		public boolean isHostile() {
			return this.hostile;
		}

		public void setHostile(final boolean hostile) {
			this.hostile = hostile;
		}

		public Vec3 getLastPosition() {
			return this.lastPosition;
		}
	}

	public enum Mode {
		/**
		 * Do not reply any request, but unlike disable, requesting signal will still be decoded.
		 */
		OFF(false, false, false, false),
		/**
		 * Do not send any request, but will reply ally's request.
		 */
		REPLY_ALLY_ONLY(false, false, true, false),
		/**
		 * Do not send any request, but will reply everyone's request.
		 */
		REPLY_ONLY(false, false, true, true),
		/**
		 * Only send request to others. Does not reply anyone.
		 */
		ASK_ONLY(true, true, false, false),
		/**
		 * Send request to others, but only reply ally's request.
		 */
		ASK_ALL_REPLY_ALLY(true, true, true, false),
		/**
		 * Only send request to known allies and reply theirs. This will ensure their positions are up to date.
		 */
		SYNC_KNOWN(true, false, true, false),
		/**
		 * Send request to all ships. And will reply everyone's request.
		 */
		BROADCAST(true, true, true, true);

		public final boolean askAlly;
		public final boolean askUnknown;
		public final boolean replyAlly;
		public final boolean replyUnknown;

		private Mode(final boolean askAlly, final boolean askUnknown, final boolean replyAlly, final boolean replyUnknown) {
			this.askAlly = askAlly;
			this.askUnknown = askUnknown;
			this.replyAlly = replyAlly;
			this.replyUnknown = replyUnknown;
		}

		public boolean shouldAsk(final boolean isAlly) {
			return isAlly && this.askAlly || this.askUnknown;
		}

		public boolean shouldReply(final boolean isAlly) {
			return isAlly && this.replyAlly || this.replyUnknown;
		}
	}

	public enum HostileMode {
		/**
		 * Do not actively mark anyone as hostile.
		 */
		NONE(false, false, false),
		/**
		 * No active hostile mark, but share other ally's hostile list.
		 */
		SHARE_ALLY(false, false, true),
		/**
		 * Mark any non ally as hostile.
		 */
		NON_ALLY(true, false, false),
		/**
		 * Mark any non ally as hostile, and share other ally's hostile list.
		 */
		NON_ALLY_WITH_SHARE(true, false, true),
		/**
		 * Mark everyone as hostile.
		 */
		EVERYONE(true, true, false);

		public final boolean markNonAlly;
		public final boolean markAlly;
		public final boolean shareWithAlly;

		private HostileMode(final boolean markNonAlly, final boolean markAlly, final boolean shareWithAlly) {
			this.markNonAlly = markNonAlly;
			this.markAlly = markAlly;
			this.shareWithAlly = shareWithAlly;
		}
	}
}
