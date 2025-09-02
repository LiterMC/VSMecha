package com.github.litermc.vsmecha.block.radar;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.util.ShipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.valkyrienskies.core.api.ships.ServerShip;

public class IFFBeaconBlockEntity extends EnergyBasedBlockEntity {
	private Mode mode = Mode.BROADCAST;

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

		if (!this.isEnabled()) {
			return;
		}

		final ServerLevel level = (ServerLevel) (this.getLevel());
		final ServerShip ship = ShipUtil.getServerShip(level, this.getBlockPos());
		if (ship == null) {
			return;
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
}
