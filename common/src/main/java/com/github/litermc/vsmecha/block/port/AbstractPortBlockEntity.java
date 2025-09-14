package com.github.litermc.vsmecha.block.port;

import com.github.litermc.vsmecha.attachment.ShipNetworkAttachment;
import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.util.BlockSourceClipContext;
import com.github.litermc.vsmecha.util.ShipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Iterator;

public abstract class AbstractPortBlockEntity<O> extends EnergyBasedBlockEntity implements IPortBlockEntity {
	private final Direction direction;
	private volatile boolean overloadMode = false;
	private boolean redstoneOverload = false;
	private String channel = "";

	private Boolean working = null; // null: not working; true: overloading; false: not overloading
	private boolean ticking = false;

	protected AbstractPortBlockEntity(final BlockEntityType<? extends AbstractPortBlockEntity<O>> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.direction = state.getValue(BlockStateProperties.FACING);
	}

	public Direction getDirection() {
		return this.direction;
	}

	public boolean getOverloadMode() {
		return this.overloadMode;
	}

	public void setOverloadMode(final boolean overloadMode) {
		if (this.overloadMode == overloadMode) {
			return;
		}
		this.overloadMode = overloadMode;
		this.sendUpdate();
	}

	@Override
	public String getPortChannel() {
		return this.channel;
	}

	public void setPortChannel(final String channel) {
		if (channel.equals(this.channel)) {
			return;
		}
		this.channel = channel;
		this.setChanged();
	}

	@Override
	public int getMaxHeatCapacity() {
		return 13000;
	}

	@Override
	public int getDangerousHeatLimit() {
		return 12000;
	}

	@Override
	public int getDefaultEnergyPriority() {
		return 400;
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
	public void neighborChanged(final Block neighbor, final BlockPos neighborPos, final boolean moving) {
		super.neighborChanged(neighbor, neighborPos, moving);
		if (!(this.getLevel() instanceof ServerLevel level)) {
			return;
		}
		final boolean wantOverload = level.hasNeighborSignal(this.getBlockPos());
		if (this.redstoneOverload) {
			if (!wantOverload) {
				this.redstoneOverload = false;
				this.setOverloadMode(false);
			}
		} else if (wantOverload && !this.overloadMode) {
			this.redstoneOverload = true;
			this.setOverloadMode(true);
		}
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.overloadMode = data.getBoolean("OverloadMode");
		this.redstoneOverload = data.getBoolean("RedstoneOverload");
		this.channel = data.getString("Channel");
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putBoolean("RedstoneOverload", this.redstoneOverload);
		data.putString("Channel", this.channel);
	}

	@Override
	protected void saveShared(final CompoundTag data) {
		super.saveShared(data);
		data.putBoolean("OverloadMode", this.overloadMode);
	}

	@Override
	public void serverTick() {
		super.serverTick();

		if (!this.isEnabled()) {
			this.working = null;
			return;
		}

		final boolean overloadMode = this.getOverloadMode();
		final int newEnergy = this.getEnergyStored() - (overloadMode ? 10 : 1);
		if (newEnergy < 0) {
			this.working = null;
			return;
		}
		this.setEnergyStored(newEnergy);
		this.working = overloadMode;
	}

	protected abstract void onPortObjectOutput(final Context<O> context);

	protected void onPortObjectInput(final Context<O> context) {
		if (this.working == null) {
			return;
		}
		if (context.getAmount() <= 0) {
			return;
		}
		if (this.ticking) {
			return;
		}
		this.ticking = true;
		try {
			this.onPortObjectInputInner(context);
		} finally {
			this.ticking = false;
		}
	}

	private void onPortObjectInputInner(final Context<O> context) {
		if (!this.working) {
			final Iterator<BlockEntity> iterator = ShipNetworkAttachment.streamAvailablePorts(this).iterator();
			while (iterator.hasNext() && context.getAmount() > 0) {
				final AbstractPortBlockEntity<O> other = (AbstractPortBlockEntity<O>) (iterator.next());
				other.onPortObjectOutput(context);
			}
			return;
		}
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos pos = this.getBlockPos();
		final ServerShip ship = ShipUtil.getServerShip(level, pos);
		Vec3 from = pos.getCenter();
		Vec3 to = from.relative(this.direction, 6);
		if (ship != null) {
			from = VSGameUtilsKt.toWorldCoordinates(ship, from);
			to = VSGameUtilsKt.toWorldCoordinates(ship, to);
		}
		final BlockHitResult hitResult = level.clip(new BlockSourceClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, pos));
		if (hitResult.getType() != HitResult.Type.BLOCK) {
			return;
		}
		final BlockEntity be = level.getBlockEntity(hitResult.getBlockPos());
		if (!(be instanceof final AbstractPortBlockEntity<?> pbe0) || pbe0.getPortType() != this.getPortType()) {
			return;
		}
		final AbstractPortBlockEntity<O> pbe = (AbstractPortBlockEntity<O>) (pbe0);
		if (pbe.getDirection() == hitResult.getDirection()) {
			return;
		}
		if (!pbe.getOverloadMode()) {
			pbe.onPortObjectOutput(context);
		}
		pbe.onPortObjectInput(context);
	}

	public static final class Context<O> {
		private final O object;
		private final boolean simulate;
		private int amount;

		/**
		 * @param object   The operating object
		 * @param amount   Amount of the object
		 * @param simulate If the operation is simulated
		 */
		public Context(final O object, final int amount, final boolean simulate) {
			this.object = object;
			this.simulate = simulate;
			this.amount = amount;
		}

		public O getObject() {
			return this.object;
		}

		public boolean isSimulated() {
			return this.simulate;
		}

		public int getAmount() {
			return this.amount;
		}

		public void consume(final int amount) {
			this.amount -= amount;
		}
	}
}
