package com.github.litermc.vsmecha.block.port;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.block.IJointPeripheralBlockEntity;
import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.compat.computercraft.port.EnergyPortPeripheral;
import com.github.litermc.vsmecha.platform.EnergyInterface;
import com.github.litermc.vsmecha.platform.PlatformHelper;
import com.github.litermc.vsmecha.util.BlockSourceClipContext;
import com.github.litermc.vsmecha.util.Counter;
import com.github.litermc.vsmecha.util.RayCastUtil;
import com.github.litermc.vsmecha.util.ShipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class EnergyPortBlockEntity extends EnergyBasedBlockEntity implements IJointPeripheralBlockEntity {
	private final Direction direction;
	private int consumed = 0;
	private int transferred = 0;
	private volatile int lastTransferred = 0;
	private volatile boolean overloadMode = false;
	private boolean redstoneOverload = false;

	public EnergyPortBlockEntity(final BlockEntityType<? extends EnergyPortBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.direction = state.getValue(BlockStateProperties.FACING);
	}

	public EnergyPortBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.ENERGY_PORT.get(), pos, state);
	}

	public boolean getOverloadMode() {
		return this.overloadMode;
	}

	public void setOverloadMode(final boolean overloadMode) {
		if (this.overloadMode == overloadMode) {
			return;
		}
		this.overloadMode = overloadMode;
		this.setChanged();
	}

	public int getLastTransferred() {
		return this.lastTransferred;
	}

	public double getTransferLoad() {
		return (double) (this.lastTransferred) / this.getEnergyOutputLimit();
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
		return -100;
	}

	@Override
	public int getMaxEnergyStorage() {
		return 10000;
	}

	@Override	
	public int getEnergyInputLimit() {
		return 0;
	}

	@Override
	public int getEnergyOutputLimit() {
		return this.getMaxEnergyStorage();
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
				this.setOverloadMode(false);
			}
		} else if (wantOverload && !this.overloadMode) {
			this.redstoneOverload = true;
			this.setOverloadMode(true);
		}
	}

	@Override
	public boolean canConnectPeripheralWire(final Direction dir) {
		return true;
	}

	@Override
	public void consumeEnergy(final int amount) {
		super.consumeEnergy(amount);
		this.consumed += amount;
	}

	@Override
	public int tickEnergyInput(final int available) {
		if (!this.isEnabled()) {
			return 0;
		}
		this.energyInputRemaining = this.getMaxEnergyStorage();
		if (available == 0) {
			return 0;
		}
		return this.overloadMode ? this.pushToRemote(available) : this.pushToNeighbour(available);
	}

	protected int pushToNeighbour(final int available) {
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos outPos = this.getBlockPos().relative(this.direction);
		final EnergyInterface ei = PlatformHelper.get().getEnergyInterface(level, outPos, this.direction.getOpposite());
		if (ei == null) {
			return 0;
		}
		final int pushed = ei.pushEnergy(Math.min(available, this.getEnergyOutputLimit()), false);
		this.transferred += pushed;
		return pushed;
	}

	protected int pushToRemote(final int available) {
		final Counter remain = new Counter(available);
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos pos = this.getBlockPos();
		final ServerShip ship = ShipUtil.getServerShip(level, pos);
		Vec3 from = pos.getCenter();
		Vec3 to = from.relative(this.direction, 5.6);
		if (ship != null) {
			from = VSGameUtilsKt.toWorldCoordinates(ship, from);
			to = VSGameUtilsKt.toWorldCoordinates(ship, to);
		}
		final LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt != null) {
			RayCastUtil.clipEntities(level, from, to, (entity) -> entity instanceof LivingEntity, (entity, p) -> {
				final int LIGHTING_COST = 8;
				if (remain.value < LIGHTING_COST) {
					return;
				}
				remain.value -= LIGHTING_COST;
				entity.thunderHit(level, bolt);
			});
		}
		if (remain.value < 1000) {
			return available - remain.value;
		}
		final BlockHitResult hitResult = level.clip(new BlockSourceClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, pos));
		if (hitResult.getType() != HitResult.Type.BLOCK) {
			return available - remain.value;
		}
		remain.value -= 10;
		final EnergyInterface ei = PlatformHelper.get().getEnergyInterface(level, hitResult.getBlockPos(), hitResult.getDirection());
		if (ei != null) {
			final int pushed = ei.pushEnergy(Math.min(remain.value, this.getEnergyOutputLimit()), false);
			this.transferred += pushed;
			remain.value -= pushed;
		}
		return available - remain.value;
	}

	@Override
	public boolean hasDirectionalEnergyStorage() {
		return true;
	}

	@Override
	public boolean canPushByExternal(final Direction dir) {
		return dir != this.direction;
	}

	@Override
	protected Object createPeripheral() {
		return new EnergyPortPeripheral(this);
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.overloadMode = data.getBoolean("OverloadMode");
		this.redstoneOverload = data.getBoolean("RedstoneOverload");
		this.lastTransferred = data.getInt("Transferred");
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putBoolean("RedstoneOverload", this.redstoneOverload);
	}

	@Override
	protected void saveShared(final CompoundTag data) {
		super.saveShared(data);
		data.putBoolean("OverloadMode", this.overloadMode);
	}

	@Override
	public CompoundTag getUpdateTag() {
		final CompoundTag data = super.getUpdateTag();
		data.putInt("Transferred", this.lastTransferred);
		return data;
	}

	@Override
	public void serverTick() {
		super.serverTick();

		this.transferHeat((this.consumed + this.transferred) / 1000);
		if (this.lastTransferred != this.transferred) {
			this.lastTransferred = this.transferred;
			this.sendUpdate();
		}
		this.consumed = 0;
		this.transferred = 0;
	}
}
