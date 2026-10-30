package com.github.litermc.vsmecha.block.radar;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.attachment.ShipNetworkAttachment;
import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.compat.computercraft.IFFBeaconPeripheral;
import com.github.litermc.vsmecha.util.ShipUtil;
import com.github.litermc.vsmecha.util.TeamUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class IFFBeaconBlockEntity extends EnergyBasedBlockEntity {
	private static final int MAX_RANGE = 16 * 8;

	private Mode mode = Mode.BROADCAST;
	private volatile HostileMode hostileMode = HostileMode.NONE;
	private final Set<UUID> whitelist = new HashSet<>();
	private final Set<UUID> hostiles = new HashSet<>();
	private final LongOpenHashSet lastKnownAllies = new LongOpenHashSet();

	private UUID owner = null;

	private volatile boolean working = false;
	private int syncTicker = -1;

	private long shipIdCache = -1;
	private AABB scanBoxCache = null;
	private final Long2ObjectMap<Identity> identityCache = new Long2ObjectOpenHashMap<>();

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

	public void markAllyShip(final long shipId) {
		Identity id = this.identityCache.get(shipId);
		if (id != null) {
			id.setStatus(HostileStatus.ALLY);
		}
	}

	public void unmarkAllyShip(final long shipId) {
		Identity id = this.identityCache.get(shipId);
		if (id != null && id.getStatus() == HostileStatus.ALLY) {
			id.setStatus(HostileStatus.UNKNOWN);
		}
	}

	public void markHostile(final UUID owner) {
		this.whitelist.remove(owner);
		this.hostiles.add(owner);
	}

	public void unmarkHostile(final UUID owner) {
		this.hostiles.remove(owner);
	}

	public void markHostileShip(final long shipId) {
		Identity id = this.identityCache.get(shipId);
		if (id != null) {
			id.setStatus(HostileStatus.HOSTILE);
		}
	}

	public void unmarkHostileShip(final long shipId) {
		Identity id = this.identityCache.get(shipId);
		if (id != null && id.getStatus() == HostileStatus.HOSTILE) {
			id.setStatus(HostileStatus.UNKNOWN);
		}
	}

	public Set<UUID> getHostiles() {
		return this.hostiles;
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
		return new IFFBeaconPeripheral(this);
	}

	public boolean isOwner(final UUID owner) {
		if (this.owner == null || owner == null) {
			return false;
		}
		return this.owner.equals(owner);
	}

	public boolean isAlly(final UUID owner) {
		return TeamUtil.isAlly(this.owner, owner);
	}

	protected AABB getScanBox(final LoadedServerShip ship) {
		if (this.scanBoxCache == null) {
			final AABBdc box = ship.getWorldAABB();
			this.scanBoxCache = new AABB(
				box.minX() - MAX_RANGE, box.minY() - MAX_RANGE, box.minZ() - MAX_RANGE,
				box.maxX() + MAX_RANGE, box.maxY() + MAX_RANGE, box.maxZ() + MAX_RANGE
			);
		}
		return this.scanBoxCache;
	}

	/**
	 * @param shipId Requestor ship ID
	 * @param owner Requestor owner
	 * @param requester Requestor block entity
	 * @return whether the IFF beacon should reply information
	 */
	public boolean onRequest(final long shipId, final UUID owner, final IFFBeaconBlockEntity requester) {
		if (!this.isEnabled()) {
			return false;
		}
		final boolean isAlly = this.isAlly(owner);
		this.onShipDetected(shipId, owner, requester, isAlly);
		if (this.mode.shouldReply(isAlly)) {
			return true;
		}
		return false;
	}

	protected void onShipDetected(final long shipId, final UUID owner, final IFFBeaconBlockEntity be, final boolean isAlly) {
		Identity id = this.identityCache.get(shipId);
		if (id != null) {
			if (id.getStatus() != HostileStatus.UNKNOWN) {
				return;
			}
		} else {
			id = new Identity(shipId, owner, HostileStatus.UNKNOWN);
			this.identityCache.put(shipId, id);
		}

		final HostileMode hostileMode = this.hostileMode;
		if (isAlly && (hostileMode.markLevel != 1 || this.isOwner(owner))) {
			id.setStatus(HostileStatus.ALLY);
			if (hostileMode.shareWithAlly) {
				for (final UUID hostOwner : be.hostiles) {
					if (!this.whitelist.contains(hostOwner)) {
						this.hostiles.add(hostOwner);
					}
				}
				for (final Identity entry : be.identityCache.values()) {
					final long entryId = entry.getId();
					if (entryId == this.shipIdCache) {
						continue;
					}
					final Identity instance = this.identityCache.get(entryId);
					if (instance == null) {
						this.identityCache.put(entryId, entry.clone());
					} else if (instance.getStatus() == HostileStatus.UNKNOWN) {
						instance.setStatus(entry.getStatus());
					}
				}
			}
		} else if (hostileMode.markLevel > 0 && !this.whitelist.contains(owner)) {
			id.setStatus(HostileStatus.HOSTILE);
			this.markHostile(owner);
		}
	}

	public boolean pingShip(final LoadedServerShip selfShip, final LoadedServerShip otherShip) {
		final BlockPos otherPos = ShipNetworkAttachment.get(otherShip).getLastIFFBeacon();
		if (otherPos == null) {
			return false;
		}
		if (!(level.getBlockEntity(otherPos) instanceof IFFBeaconBlockEntity otherIff)) {
			return false;
		}
		if (!otherIff.onRequest(selfShip.getId(), this.owner, this)) {
			return false;
		}
		final UUID otherOwner = otherIff.getOwner();
		this.onShipDetected(otherShip.getId(), otherOwner, otherIff, this.isAlly(otherOwner));
		return true;
	}

	protected void pingAllAround(final LoadedServerShip ship) {
		for (final Ship other : VSGameUtilsKt.getShipsIntersecting(this.getLevel(), this.getScanBox(ship))) {
			if (!(other instanceof LoadedServerShip loadedOther)) {
				continue;
			}
			this.pingShip(ship, loadedOther);
		}
	}

	protected void syncKnownAllies(final LoadedServerShip ship) {
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final VsiServerShipWorld world = VSGameUtilsKt.getShipObjectWorld(level);

		this.lastKnownAllies.clear();
		for (Identity entry : this.identityCache.values()) {
			if (entry.getStatus() == HostileStatus.ALLY) {
				this.lastKnownAllies.add(entry.getId());
			}
		}

		for (final long targetShipId : this.lastKnownAllies) {
			final LoadedServerShip targetShip = world.getLoadedShips().getById(targetShipId);
			if (targetShip == null) {
				continue;
			}
			final AABBdc otherBox = targetShip.getWorldAABB();
			if (!this.getScanBox(ship).intersects(
				otherBox.minX(), otherBox.minY(), otherBox.minZ(),
				otherBox.maxX(), otherBox.maxY(), otherBox.maxZ()
			)) {
				continue;
			}
			this.pingShip(ship, targetShip);
		}
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.mode = Mode.values()[data.getByte("Mode")];
		this.hostileMode = HostileMode.values()[data.getByte("HostileMode")];
	}

	@Override
	public void saveAdditional(final CompoundTag data) {
		super.load(data);
		data.putByte("Mode", (byte) (this.mode.ordinal()));
		data.putByte("HostileMode", (byte) (this.hostileMode.ordinal()));
	}

	@Override
	public void serverTick() {
		super.serverTick();

		this.scanBoxCache = null;

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
		if (pos.equals(ShipNetworkAttachment.get(ship).getLastIFFBeacon())) {
			this.working = false;
			return;
		}
		this.working = true;

		this.shipIdCache = ship.getId();
		if (!this.mode.shouldAsk(true)) {
			return;
		}
		if (this.syncTicker <= 0) {
			final boolean firstTick = this.syncTicker < 0;
			this.syncTicker = 20;
			if (this.isDangerous()) {
				final int exceed = this.getHeat() - this.getDangerousHeatLimit();
				this.syncTicker += exceed * 280 / (this.getMaxHeatCapacity() - this.getDangerousHeatLimit());
			}
			if (!firstTick) {
				if (this.mode.shouldAsk(false)) {
					this.pingAllAround(ship);
				} else {
					this.syncKnownAllies(ship);
				}
				this.scanBoxCache = null;
			}
		} else {
			this.syncTicker--;
		}
	}

	public static final class Identity {
		private final long id;
		private final UUID owner;
		private HostileStatus status;
		private Vec3 lastPosition = null;

		public Identity(final long id, final UUID owner, final HostileStatus status) {
			this.id = id;
			this.owner = owner;
			this.status = status;
		}

		public long getId() {
			return this.id;
		}

		@Nullable
		public UUID getOwner() {
			return this.owner;
		}

		public HostileStatus getStatus() {
			return this.status;
		}

		public void setStatus(final HostileStatus status) {
			this.status = status;
		}

		public Vec3 getLastPosition() {
			return this.lastPosition;
		}

		@Override
		public Identity clone() {
			final Identity cloned = new Identity(this.id, this.owner, this.status);
			cloned.lastPosition = this.lastPosition;
			return cloned;
		}
	}

	public enum Mode {
		/**
		 * Do not reply any request, but unlike disable, requesting signal will still be decoded.
		 */
		QUIET(2, 2),
		/**
		 * Do not send any request, but will reply ally's request.
		 */
		REPLY_ALLY_ONLY(2, 1),
		/**
		 * Do not send any request, but will reply everyone's request.
		 */
		REPLY_ONLY(2, 0),
		/**
		 * Only send request to others. Does not reply anyone.
		 */
		ASK_ONLY(0, 2),
		/**
		 * Send request to others, but only reply ally's request.
		 */
		ASK_ALL_REPLY_ALLY(0, 1),
		/**
		 * Only send request to known allies and reply theirs. This will ensure their positions are up to date.
		 */
		SYNC_KNOWN(1, 1),
		/**
		 * Send request to all ships. And will reply everyone's request.
		 */
		BROADCAST(0, 0);

		public final int askLevel;
		public final int replyLevel;

		private Mode(final int askLevel, final int replyLevel) {
			this.askLevel = askLevel;
			this.replyLevel = replyLevel;
		}

		public boolean shouldAsk(final boolean isAlly) {
			return this.askLevel < (isAlly ? 2 : 1);
		}

		public boolean shouldReply(final boolean isAlly) {
			return this.replyLevel < (isAlly ? 2 : 1);
		}
	}

	public enum HostileMode {
		/**
		 * Do not mark anyone as hostile.
		 */
		NONE(0, false),
		/**
		 * No active hostile mark, but share other ally's hostile list.
		 */
		SHARE_ALLY(0, true),
		/**
		 * Mark any non ally as hostile.
		 */
		NON_ALLY(1, false),
		/**
		 * Mark any non ally as hostile, and share other ally's hostile list.
		 */
		NON_ALLY_WITH_SHARE(1, true),
		/**
		 * Mark everyone as hostile.
		 */
		EVERYONE(2, false);

		public final int markLevel;
		public final boolean shareWithAlly;

		private HostileMode(final int markLevel, final boolean shareWithAlly) {
			this.markLevel = markLevel;
			this.shareWithAlly = shareWithAlly;
		}
	}

	public enum HostileStatus {
		ALLY, HOSTILE, UNKNOWN;
	}
}
