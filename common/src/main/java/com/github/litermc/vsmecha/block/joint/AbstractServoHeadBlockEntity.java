package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.block.BaseBlockEntity;
import com.github.litermc.vsmecha.block.IJointPeripheralBlockEntity;
import com.github.litermc.vsmecha.block.IPhysTickableBlockEntity;
import com.github.litermc.vsmecha.compat.CompatMods;
import com.github.litermc.vsmecha.compat.computercraft.ServoHeadPeripheral;
import com.github.litermc.vsmecha.util.ShipPeripheralHolder;
import com.github.litermc.vsmecha.util.ShipUtil;
import com.github.litermc.vtil.util.TaskUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.core.internal.world.VsiPhysLevel;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public abstract class AbstractServoHeadBlockEntity extends BaseBlockEntity implements IJointBlockEntity, IJointPeripheralBlockEntity, IPhysTickableBlockEntity {
	private final Direction direction;
	BlockPos basePos = null;
	private volatile AbstractServoBlockEntity sbe = null;
	AbstractServoBlockEntity.ServoInfo servoInfo = null;

	private final ShipPeripheralHolder peripheralHolder = new ShipPeripheralHolder(this, () -> new ServoHeadPeripheral(this));

	public AbstractServoHeadBlockEntity(final BlockEntityType<? extends AbstractServoHeadBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.direction = state.getValue(BlockStateProperties.FACING);
	}

	public Direction getDirection() {
		return this.direction;
	}

	@Override
	public BlockPos getAttachingBlock() {
		return this.basePos;
	}

	@Override
	public ServerShip getPeerShip() {
		if (this.basePos == null) {
			return null;
		}
		return ShipUtil.getServerShip((ServerLevel) (this.getLevel()), this.basePos);
	}

	@Override
	public boolean canTransferEnergy() {
		return this.basePos != null && this.getLevel().getBlockEntity(this.basePos) instanceof final AbstractServoBlockEntity sbe && sbe.canTransferEnergy();
	}

	@Override
	public final ShipPeripheralHolder getShipPeripheralHolder() {
		return this.peripheralHolder;
	}

	@Override
	public boolean canConnectPeripheralWire(final Direction dir) {
		return this.direction.getOpposite() == dir;
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		if (CompatMods.COMPUTERCRAFT.isLoaded()) {
			this.peripheralHolder.load(data);
		}
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		if (CompatMods.COMPUTERCRAFT.isLoaded()) {
			this.peripheralHolder.save(data);
		}
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (CompatMods.COMPUTERCRAFT.isLoaded()) {
			this.peripheralHolder.onSetLevel(serverLevel);
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (CompatMods.COMPUTERCRAFT.isLoaded()) {
			this.peripheralHolder.onRemove();
		}
	}

	@Override
	public void serverTick() {
		super.serverTick();

		if (this.basePos == null) {
			return;
		}
		if (this.servoInfo.detached()) {
			this.basePos = null;
			this.servoInfo = null;
			return;
		}

		final ServerLevel level = (ServerLevel) (this.getLevel());
		if (level.getBlockEntity(this.basePos) instanceof final AbstractServoBlockEntity sbe && sbe.servoInfo == this.servoInfo) {
			this.sbe = sbe;
			return;
		}
		this.sbe = null;
		final AbstractServoBlockEntity.ServoInfo servoInfo = this.servoInfo;
		TaskUtil.queuePhysicsTick(level, (world) -> servoInfo.detach((VsiPhysLevel) world));
		this.servoInfo = null;
		this.basePos = null;
	}

	@Override
	public void physicsTick(final PhysShip ship, final PhysLevel world) {
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos basePos = this.basePos;
		if (basePos == null) {
			return;
		}
		final ServerShip peerShip = VSGameUtilsKt.getShipManagingPos(level, basePos);
		if (peerShip != null && !peerShip.isStatic()) {
			return;
		}
		final AbstractServoBlockEntity sbe = this.sbe;
		if (sbe == null || sbe.isRemoved()) {
			return;
		}
		sbe.stepServo(world, peerShip == null ? null : world.getShipById(peerShip.getId()), ship, 1.0 / 60);
	}
}
