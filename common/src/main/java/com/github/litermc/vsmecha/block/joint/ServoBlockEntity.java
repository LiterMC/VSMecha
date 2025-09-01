package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.block.IJointPeripheralBlockEntity;
import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.compat.CompatMods;
import com.github.litermc.vsmecha.compat.computercraft.ServoPeripheral;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipModemPeripheral;
import com.github.litermc.vsmecha.util.MathUtil;
import com.github.litermc.vsmecha.util.ShipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.joml.AxisAngle4d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.apigame.constraints.VSFixedOrientationConstraint;
import org.valkyrienskies.core.apigame.constraints.VSHingeOrientationConstraint;
import org.valkyrienskies.core.apigame.constraints.VSHingeTargetAngleConstraint;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import dan200.computercraft.api.network.wired.WiredNode;

public class ServoBlockEntity extends EnergyBasedBlockEntity implements IAttachableBlockEntity, IJointPeripheralBlockEntity {
	private static final Vector3dc ZERO_VEC3 = new Vector3d();
	private static final Quaterniondc FREEROT_QUAT = new Quaterniond(new AxisAngle4d(Math.PI / 2, 0, 0, 1));
	private static final double ATTACH_COMPLIANCE = 0;
	private static final double ROTATE_COMPLIANCE = 1e-11;

	private final Direction direction;
	private BlockPos headPos = null;
	BlockPos pendingHeadPos = null;
	ServoInfo servoInfo = null;
	private volatile boolean autoAttach = true;
	private volatile boolean working = false;
	private volatile double angle = 0;
	private volatile double workingAngle = 0;
	private volatile double targetAngle = 0;

	private int autoAttachCD = 20;
	private Object headNode = null;

	public ServoBlockEntity(final BlockEntityType<? extends ServoBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.direction = state.getValue(BlockStateProperties.FACING);
	}

	public ServoBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.SERVO.get(), pos, state);
	}

	/**
	 * @return max rotation speed in rad/t
	 */
	public double getMaxRotateSpeed() {
		return 10 * Math.PI / 180;
	}

	public Direction getDirection() {
		return this.direction;
	}

	@Override
	public BlockPos getAttachingBlock() {
		return this.headPos;
	}

	@Override
	public ServerShip getPeerShip() {
		if (this.headPos == null) {
			return null;
		}
		return ShipUtil.getServerShip((ServerLevel) (this.getLevel()), this.headPos);
	}

	@Override
	public boolean canTransferEnergy() {
		return this.isEnabled();
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

	public double getCurrentAngle() {
		return this.angle;
	}

	public double getLastWorkingAngle() {
		return this.workingAngle;
	}

	public double getTargetAngle() {
		return this.targetAngle;
	}

	public void setTargetAngle(double angle) {
		angle = MathUtil.normalizeAngle(angle);
		if (this.targetAngle == angle) {
			return;
		}
		this.targetAngle = angle;
		this.setChanged();
	}

	public int getEnergyConsumption() {
		return 1000;
	}

	@Override
	public int getMaxHeatCapacity() {
		return 24000;
	}

	@Override
	public int getDangerousHeatLimit() {
		return 20000;
	}

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
		}
		this.targetAngle = MathUtil.normalizeAngle(data.getDouble("TargetAngle"));
	}

	@Override
	protected void saveShared(final CompoundTag data) {
		super.saveShared(data);
		data.putBoolean("AutoAttach", this.autoAttach);
		final BlockPos headPos = this.headPos != null ? this.headPos : this.pendingHeadPos;
		if (headPos != null) {
			data.putIntArray("HeadPos", new int[]{headPos.getX(), headPos.getY(), headPos.getZ()});
		}
		data.putDouble("TargetAngle", this.targetAngle);
	}

	private double readAngle() {
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final ServoHeadBlockEntity head = (ServoHeadBlockEntity) (level.getBlockEntity(this.headPos));
		final ServerShip ship = ShipUtil.getServerShip(level, this.getBlockPos());
		final ServerShip other = ShipUtil.getServerShip(level, this.headPos);
		final Direction dir = this.getDirection();
		final Vector3dc dirVec = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());
		final Quaterniond relRot = ShipUtil.getShipRelativeRotation(ship, other)
			.mul(new Quaterniond(dir.getRotation()).invert().mul(new Quaterniond(head.getDirection().getOpposite().getRotation())))
			.normalize();
		final double dot = dirVec.dot(relRot.x, relRot.y, relRot.z);
		return MathUtil.normalizeAngle(-2 * Math.atan2(dot, relRot.w));
	}

	@Override
	public boolean attachTo(final BlockPos otherPos) {
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos pos = this.getBlockPos();

		if (!(level.getBlockEntity(otherPos) instanceof final ServoHeadBlockEntity head)) {
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

		if (this.headPos != null) {
			this.detach();
		}

		this.headPos = otherPos;

		final double angle = this.readAngle();
		this.angle = angle;
		this.workingAngle = angle;

		final VSAttachmentConstraint attachConstraint1 = new VSAttachmentConstraint(
			selfId,
			otherId,
			ATTACH_COMPLIANCE,
			new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
			new Vector3d(otherPos.getX() + 0.5, otherPos.getY() + 0.5, otherPos.getZ() + 0.5),
			Double.POSITIVE_INFINITY,
			0
		);
		final VSConstraint attachConstraint2 = this.createFreeRotationConstraint();
		final VSConstraint rotateConstraint = this.createRotationConstraint();
		this.servoInfo = new ServoInfo();
		this.servoInfo.attachConstraint1Id = world.createNewConstraint(attachConstraint1);
		this.servoInfo.attachConstraint2Id = world.createNewConstraint(attachConstraint2);
		this.servoInfo.rotateConstraintId = world.createNewConstraint(rotateConstraint);

		head.basePos = pos;
		head.servoInfo = this.servoInfo;
		this.working = true;
		this.setChanged();
		return true;
	}

	protected VSConstraint createRotationConstraint() {
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final ServoHeadBlockEntity head = (ServoHeadBlockEntity) (level.getBlockEntity(this.headPos));
		final Quaterniondc dir = new Quaterniond(this.getDirection().getRotation());
		final Quaterniond rotation = new Quaterniond(head.getDirection().getOpposite().getRotation())
			.mul(new Quaterniond(new AxisAngle4d(this.workingAngle, 0, 1, 0)));
		return new VSFixedOrientationConstraint(
			ShipUtil.getShipOrDimId(level, this.getBlockPos()),
			ShipUtil.getShipOrDimId(level, this.headPos),
			ROTATE_COMPLIANCE,
			dir,
			rotation,
			Double.POSITIVE_INFINITY
		);
	}

	protected VSConstraint createFreeRotationConstraint() {
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final ServoHeadBlockEntity head = (ServoHeadBlockEntity) (level.getBlockEntity(this.headPos));
		final Quaterniondc baseRot = new Quaterniond(this.getDirection().getRotation()).mul(FREEROT_QUAT);
		final Quaterniond otherRot = new Quaterniond(head.getDirection().getOpposite().getRotation()).mul(FREEROT_QUAT);
		return new VSHingeOrientationConstraint(
			ShipUtil.getShipOrDimId(level, this.getBlockPos()),
			ShipUtil.getShipOrDimId(level, this.headPos),
			ATTACH_COMPLIANCE,
			baseRot,
			otherRot,
			Double.POSITIVE_INFINITY
		);
	}

	private void disconnectHeadNode() {
		if (CompatMods.COMPUTERCRAFT.isLoaded() && this.headNode != null) {
			final WiredNode selfNode = ((ShipModemPeripheral) (this.getShipModemPeripheral())).getElement().getNode();
			selfNode.disconnectFrom((WiredNode) (this.headNode));
			this.headNode = null;
		}
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
		this.autoAttachCD = 20 * 3;
		this.setChanged();
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
	protected Object createPeripheral() {
		return new ServoPeripheral(this);
	}

	@Override
	public boolean canConnectPeripheralWire(final Direction dir) {
		return this.direction != dir;
	}

	@Override
	public void serverTick() {
		super.serverTick();

		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos pos = this.getBlockPos();
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(level);
		if (this.headPos != null) {
			if (this.servoInfo.detached) {
				this.disconnectHeadNode();
				this.servoInfo = null;
				this.headPos = null;
				this.setChanged();
			} else if (
				!(level.getBlockEntity(this.headPos) instanceof ServoHeadBlockEntity) ||
				ShipUtil.getShipOrDimId(level, pos) == ShipUtil.getShipOrDimId(level, this.headPos)
			) {
				this.detach();
			}
		}
		if (!this.isAttached()) {
			if (this.pendingHeadPos != null) {
				this.attachTo(this.pendingHeadPos);
				this.pendingHeadPos = null;
				this.setChanged();
				return;
			}
			if (this.getAutoAttach()) {
				if (this.autoAttachCD <= 0) {
					this.tryAttach();
					this.autoAttachCD = 20;
				} else {
					this.autoAttachCD--;
				}
			} else {
				this.autoAttachCD = 20;
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

		double maxSpeed = this.getMaxRotateSpeed();
		if (this.isDangerous()) {
			maxSpeed /= 2;
		}
		final double angle = this.readAngle();
		final double targetAngle = this.getTargetAngle();
		final boolean wasWorking = this.working;
		this.angle = angle;

		boolean canWork = this.isEnabled();
		if (canWork) {
			final int newEnergy = this.getEnergyStored() - this.getEnergyConsumption();
			canWork = newEnergy >= 0;
			if (canWork) {
				this.setEnergyStored(newEnergy);
			}
		}

		if (canWork) {
			double diff = MathUtil.normalizeAngle(targetAngle - angle);
			if (Math.abs(diff) < 0.01) {
				diff = 0;
			}
			if (diff != 0 || !wasWorking) {
				double newWorkingAngle = Math.abs(diff) <= maxSpeed
					? targetAngle
					: MathUtil.normalizeAngle(angle + (diff > 0 ? maxSpeed : -maxSpeed));
				if (wasWorking) {
					final double lastWorkingAngle = this.workingAngle;
					newWorkingAngle = MathUtil.lerpAngle(newWorkingAngle, lastWorkingAngle, 0.5);
					final int heat = ((int) (Math.abs(MathUtil.normalizeAngle(lastWorkingAngle - angle)) / Math.PI * 100)) * 10;
					this.transferHeat(heat);
				}
				this.workingAngle = newWorkingAngle;
				world.updateConstraint(this.servoInfo.rotateConstraintId, this.createRotationConstraint());
				this.working = true;
			}
		} else if (wasWorking) {
			world.updateConstraint(this.servoInfo.rotateConstraintId, this.createFreeRotationConstraint());
			this.working = false;
		}
	}

	static final class ServoInfo {
		boolean detached = false;
		int attachConstraint1Id, attachConstraint2Id;
		int rotateConstraintId;

		void detach(final ServerShipWorldCore world) {
			world.removeConstraint(this.attachConstraint1Id);
			world.removeConstraint(this.attachConstraint2Id);
			world.removeConstraint(this.rotateConstraintId);
			this.detached = true;
		}
	}
}
