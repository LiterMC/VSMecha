package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.block.IJointPeripheralBlockEntity;
import com.github.litermc.vsmecha.block.IPhysTickableBlockEntity;
import com.github.litermc.vsmecha.compat.CompatMods;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipModemPeripheral;
import com.github.litermc.vsmecha.util.ShipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import dan200.computercraft.api.network.wired.WiredNode;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public abstract class AbstractServoBlockEntity extends JointBasedBlockEntity implements IAttachableBlockEntity, IJointPeripheralBlockEntity, IPhysTickableBlockEntity {
	private static final int AUTO_ATTACH_CD = 20;

	private final Direction direction;
	protected BlockPos headPos = null;
	protected Direction headDirOppo = null;
	private volatile Quaterniondc relOrientation = null;
	private BlockPos pendingHeadPos = null;
	ServoInfo servoInfo = null;

	private volatile boolean autoAttach = true;
	private volatile boolean working = false;
	private volatile boolean positionMode = true;
	private volatile int positionLoopScale = 6;
	private int autoAttachCD = 20;
	private Object headNode = null;

	protected final AtomicInteger heatBuilt = new AtomicInteger();
	private int posPIDCD = 0;

	protected AbstractServoBlockEntity(final BlockEntityType<? extends AbstractServoBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.direction = state.getValue(BlockStateProperties.FACING);
	}

	public Direction getDirection() {
		return this.direction;
	}

	@Override
	public BlockPos getAttachingBlock() {
		return this.headPos;
	}

	@Override
	public boolean canTransferEnergy() {
		return true;
	}

	public boolean getAutoAttach() {
		return this.autoAttach;
	}

	public void setAutoAttach(final boolean autoAttach) {
		if (this.autoAttach == autoAttach) {
			return;
		}
		this.autoAttach = autoAttach;
		this.setChanged();
	}

	public boolean isWorking() {
		return this.working;
	}

	public boolean getPositionMode() {
		return this.positionMode;
	}

	public void setPositionMode(final boolean positionMode) {
		if (this.positionMode == positionMode) {
			return;
		}
		this.positionMode = positionMode;
		this.setChanged();
	}

	public int getPositionLoopScale() {
		return this.positionLoopScale;
	}

	public void setPositionLoopScale(int positionLoopScale) {
		positionLoopScale = Math.max(positionLoopScale, 1);
		if (this.positionLoopScale == positionLoopScale) {
			return;
		}
		this.positionLoopScale = positionLoopScale;
		this.setChanged();
	}

	protected final Quaterniond readRotation() {
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final ServerShip selfShip = ShipUtil.getServerShip(level, this.getBlockPos());
		final ServerShip otherShip = ShipUtil.getServerShip(level, this.headPos);
		final Direction dir = this.getDirection();
		final Vector3dc dirVec = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());
		return ShipUtil.getShipRelativeRotation(selfShip, otherShip).mul(this.relOrientation).normalize();
	}

	protected final Quaterniond readRotationInPhy(final PhysShip selfShip, final PhysShip otherShip) {
		final Direction dir = this.getDirection();
		final Vector3dc dirVec = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());
		return ShipUtil.getShipRelativeRotation(selfShip, otherShip).mul(this.relOrientation).normalize();
	}

	protected void disconnectHeadNode() {
		if (CompatMods.COMPUTERCRAFT.isLoaded() && this.headNode != null) {
			final WiredNode selfNode = ((ShipModemPeripheral) (this.getShipModemPeripheral())).getElement().getNode();
			selfNode.disconnectFrom((WiredNode) (this.headNode));
			this.headNode = null;
		}
	}

	protected abstract boolean canAttachHead(Class<?> headClass);

	@Override
	public boolean attachTo(final BlockPos otherPos) {
		if (this.headPos != null) {
			return false;
		}

		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos pos = this.getBlockPos();

		if (!(level.getBlockEntity(otherPos) instanceof final AbstractServoHeadBlockEntity head) || !this.canAttachHead(head.getClass())) {
			return false;
		}
		if (head.servoInfo != null) {
			return false;
		}

		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(level);
		final ServerShip ship = ShipUtil.getServerShip(level, pos);
		final ServerShip other = ShipUtil.getServerShip(level, otherPos);

		final long selfId = ShipUtil.getShipOrDimId(level, ship);
		final long otherId = ShipUtil.getShipOrDimId(level, other);
		if (selfId == otherId) {
			return false;
		}

		this.headPos = otherPos;
		this.headDirOppo = head.getDirection().getOpposite();
		this.setChanged();

		this.relOrientation = new Quaterniond(this.getDirection().getRotation())
			.invert()
			.mul(new Quaterniond(this.headDirOppo.getRotation()));

		final VSConstraint[] constraints = this.rebuildConstraintsFor(selfId, otherId);
		final int[] constraintIds = new int[constraints.length];
		for (int i = 0; i < constraints.length; i++) {
			constraintIds[i] = world.createNewConstraint(constraints[i]);
		}

		this.servoInfo = new ServoInfo();
		this.servoInfo.attachConstraints = constraintIds;
		return true;
	}

	@Override
	public boolean tryAttach() {
		if (this.headPos != null) {
			return false;
		}
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos pos = this.getBlockPos();
		final Vector3d attachPos = VSGameUtilsKt.toWorldCoordinates(level, new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
		for (final Vector3d p : VSGameUtilsKt.transformToNearbyShipsAndWorld(level, attachPos.x, attachPos.y, attachPos.z, 1)) {
			if (this.attachTo(BlockPos.containing(p.x, p.y, p.z))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean detach() {
		if (this.pendingHeadPos != null) {
			this.pendingHeadPos = null;
			this.setChanged();
		}
		if (this.headPos == null) {
			return false;
		}
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(level);

		this.disconnectHeadNode();

		this.servoInfo.detach(world);
		this.servoInfo = null;
		this.headPos = null;
		this.headDirOppo = null;
		this.autoAttachCD = 20 * 3;
		this.setChanged();
		return true;
	}

	public abstract int getEnergyConsumption();

	@Override
	public int getMaxEnergyStorage() {
		return this.getEnergyConsumption() * 2;
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
	public void load(final CompoundTag data) {
		super.load(data);
		this.autoAttach = data.getBoolean("AutoAttach");
		if (data.contains("HeadPos")) {
			final int[] headPosArr = data.getIntArray("HeadPos");
			this.pendingHeadPos = new BlockPos(headPosArr[0], headPosArr[1], headPosArr[2]);
			this.headDirOppo = Direction.values()[data.getByte("HeadDir")];
		}
		this.positionMode = data.getBoolean("PositionMode");
		this.positionLoopScale = Math.max(data.getInt("PositionLoopScale"), 1);
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putBoolean("AutoAttach", this.autoAttach);
		data.putBoolean("PositionMode", this.positionMode);
		data.putInt("PositionLoopScale", this.positionLoopScale);
	}

	@Override
	protected void saveShared(final CompoundTag data) {
		super.saveShared(data);
		final BlockPos headPos = this.headPos != null ? this.headPos : this.pendingHeadPos;
		if (headPos != null) {
			data.putIntArray("HeadPos", new int[]{headPos.getX(), headPos.getY(), headPos.getZ()});
			data.putByte("HeadDir", (byte) (this.headDirOppo.ordinal()));
		}
	}

	@Override
	protected int[] getConstraints() {
		return this.servoInfo == null ? null : this.servoInfo.attachConstraints;
	}

	@Override
	protected void rebuildConstraints() {
		final BlockPos headPos = this.pendingHeadPos;
		if (headPos == null) {
			return;
		}
		this.pendingHeadPos = null;

		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos pos = this.getBlockPos();

		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(level);
		final ServerShip ship = ShipUtil.getServerShip(level, pos);
		final ServerShip other = ShipUtil.getServerShip(level, headPos);

		final long selfId = ShipUtil.getShipOrDimId(level, ship);
		final long otherId = ShipUtil.getShipOrDimId(level, other);
		if (selfId == otherId) {
			return;
		}

		this.headPos = headPos;
		this.relOrientation = new Quaterniond(this.getDirection().getRotation())
			.invert()
			.mul(new Quaterniond(this.headDirOppo.getRotation()));

		final VSConstraint[] constraints = this.rebuildConstraintsFor(selfId, otherId);
		final int[] constraintIds = new int[constraints.length];
		for (int i = 0; i < constraints.length; i++) {
			constraintIds[i] = world.createNewConstraint(constraints[i]);
		}

		this.servoInfo = new ServoInfo();
		this.servoInfo.attachConstraints = constraintIds;
	}

	protected abstract VSConstraint[] rebuildConstraintsFor(long selfId, long otherId);

	@Override
	protected void removeConstriants() {
		super.removeConstriants();
		if (this.servoInfo == null) {
			return;
		}
		this.servoInfo.attachConstraints = null;
		this.servoInfo = null;
	}

	@Override
	public void serverTick() {
		super.serverTick();

		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos pos = this.getBlockPos();
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(level);
		if (this.headPos != null) {
			if (this.servoInfo.detached()) {
				this.disconnectHeadNode();
				this.servoInfo = null;
				this.headPos = null;
				this.setChanged();
			} else if (
				level.getBlockEntity(this.headPos) instanceof final AbstractServoHeadBlockEntity head &&
				ShipUtil.getShipOrDimId(level, pos) != ShipUtil.getShipOrDimId(level, this.headPos)
			) {
				if (head.basePos == null) {
					head.basePos = pos;
					head.servoInfo = this.servoInfo;
				}
			} else {
				this.detach();
			}
		}
		if (!this.isAttached()) {
			this.working = false;
			if (this.getAutoAttach()) {
				if (this.autoAttachCD <= 0) {
					this.tryAttach();
					this.autoAttachCD = AUTO_ATTACH_CD;
				} else {
					this.autoAttachCD--;
				}
			} else {
				this.autoAttachCD = AUTO_ATTACH_CD;
			}
			return;
		}

		if (
			CompatMods.COMPUTERCRAFT.isLoaded() &&
			this.headNode == null &&
			level.getBlockEntity(this.headPos) instanceof final ServoHeadBlockEntity head &&
			this.getShipModemPeripheral() instanceof final ShipModemPeripheral selfModem &&
			head.getShipModemPeripheral() instanceof final ShipModemPeripheral headModem
		) {
			final WiredNode selfNode = selfModem.getElement().getNode();
			final WiredNode headNode = headModem.getElement().getNode();
			selfNode.connectTo(headNode);
			this.headNode = headNode;
		}

		boolean canWork = this.isEnabled();
		if (canWork) {
			final int newEnergy = this.getEnergyStored() - this.getEnergyConsumption();
			canWork = newEnergy >= 0;
			if (canWork) {
				this.setEnergyStored(newEnergy);
			}
		}
		this.working = canWork;

		this.transferHeat(this.heatBuilt.getAndSet(0));
	}

	@Override
	public void physicsTick(final PhysShip ship, final Function<Long, PhysShip> lookup) {
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos headPos = this.headPos;
		if (headPos == null) {
			return;
		}
		final ServerShip otherSShip = VSGameUtilsKt.getShipManagingPos(level, headPos);
		final PhysShip otherShip = otherSShip == null ? null : lookup.apply(otherSShip.getId());

		this.stepServo(ship, otherShip, 1.0 / 60);
	}

	public abstract void stepServo(PhysShip ship, PhysShip otherShip, double dt);

	protected boolean preTickPositionPID() {
		if (!this.positionMode) {
			return false;
		}
		this.posPIDCD--;
		if (this.posPIDCD > 0) {
			return false;
		}
		this.posPIDCD = this.positionLoopScale;
		return true;
	}

	static final class ServoInfo {
		int[] attachConstraints;

		boolean detached() {
			return this.attachConstraints == null;
		}

		void detach(final ServerShipWorldCore world) {
			if (this.attachConstraints == null) {
				return;
			}
			for (final int id : this.attachConstraints) {
				world.removeConstraint(id);
			}
			this.attachConstraints = null;
		}
	}
}
