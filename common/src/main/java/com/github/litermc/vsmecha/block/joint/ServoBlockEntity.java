package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.block.IJointPeripheralBlockEntity;
import com.github.litermc.vsmecha.block.IPhysTickableBlockEntity;
import com.github.litermc.vsmecha.compat.CompatMods;
import com.github.litermc.vsmecha.compat.computercraft.ServoPeripheral;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipModemPeripheral;
import com.github.litermc.vsmecha.util.MathUtil;
import com.github.litermc.vsmecha.util.ShipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.joml.AxisAngle4d;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.apigame.constraints.VSHingeOrientationConstraint;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import dan200.computercraft.api.network.wired.WiredNode;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class ServoBlockEntity extends JointBasedBlockEntity implements IAttachableBlockEntity, IJointPeripheralBlockEntity, IPhysTickableBlockEntity {
	private static final Quaterniondc FREEROT_QUAT = new Quaterniond(new AxisAngle4d(Math.PI / 2, 0, 0, 1));
	private static final double ATTACH_COMPLIANCE = 0;
	private static final double ATTACH_MAX_FORCE = Double.POSITIVE_INFINITY;
	// private static final double ROTATE_COMPLIANCE = 0;
	private static final double ROTATE_MAX_FORCE = 1e13;

	private final Direction direction;
	private BlockPos headPos = null;
	private Direction headDirOppo = null;
	private volatile Quaterniondc relOrientation = null;
	private BlockPos pendingHeadPos = null;
	ServoInfo servoInfo = null;

	private volatile boolean autoAttach = true;
	private volatile boolean working = false;
	private volatile double angle = 0;
	private volatile boolean positionMode = true;
	private volatile double targetAngle = 0;
	private volatile double targetVelocity = 0;
	private volatile double lastTorque = 0;
	private volatile AnglePID posPID = new AnglePID(3, 0, 4, this.getMaxRotateSpeed());
	private volatile VelocityPID velPID = new VelocityPID(3e5, 1e2, 0, ROTATE_MAX_FORCE);
	private volatile double feedForwardForce = 0;
	// private volatile GravityFeedForwarder gravityFeedForwarder = new GravityFeedForwarder(0.02);

	private int autoAttachCD = 20;
	private Object headNode = null;

	private final AtomicInteger heatBuilt = new AtomicInteger();
	private int physTick = 0;
	private double lastAngle = 0;

	public ServoBlockEntity(final BlockEntityType<? extends ServoBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.direction = state.getValue(BlockStateProperties.FACING);
	}

	public ServoBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.SERVO.get(), pos, state);
	}

	/**
	 * @return max rotation speed in rad/s
	 */
	public double getMaxRotateSpeed() {
		return 15 * Math.PI / 180;
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

	public double getCurrentAngle() {
		return this.angle;
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

	public double getTargetVelocity() {
		return this.targetVelocity;
	}

	public void setTargetVelocity(final double velocity) {
		if (this.targetVelocity == velocity) {
			return;
		}
		this.targetVelocity = velocity;
		this.setChanged();
	}

	public double getLastTorque() {
		return this.lastTorque;
	}

	public PID getPosPID() {
		return this.posPID;
	}

	public PID getVelPID() {
		return this.velPID;
	}

	private void setPosPIDNoSave(final double p, final double i, final double d) {
		this.posPID = new AnglePID(p, i, d, this.getMaxRotateSpeed());
	}

	public void setPosPID(final double p, final double i, final double d) {
		this.setPosPIDNoSave(p, i, d);
		this.setChanged();
	}

	private void setVelPIDNoSave(final double p, final double i, final double d) {
		this.velPID = new VelocityPID(p, i, d, ROTATE_MAX_FORCE);
	}

	public void setVelPID(final double p, final double i, final double d) {
		this.setVelPIDNoSave(p, i, d);
		this.setChanged();
	}

	// public double getFeedForwardAlpha() {
	// 	return this.gravityFeedForwarder.alpha;
	// }

	// public void setFeedForwardAlpha(final double alpha) {
	// 	this.gravityFeedForwarder = new GravityFeedForwarder(alpha);
	// }

	public double getFeedForwardForce() {
		return this.feedForwardForce;
	}

	public void setFeedForwardForce(final double feedForwardForce) {
		this.feedForwardForce = feedForwardForce;
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
			this.headDirOppo = Direction.values()[data.getByte("HeadDir")];
		}
		this.positionMode = data.getBoolean("PositionMode");
		this.targetAngle = MathUtil.normalizeAngle(data.getDouble("TargetAngle"));
		this.targetVelocity = data.getDouble("TargetVelocity");
		final CompoundTag posPID = data.getCompound("PosPID");
		this.setPosPIDNoSave(posPID.getDouble("P"), posPID.getDouble("I"), posPID.getDouble("D"));
		final CompoundTag velPID = data.getCompound("VelPID");
		this.setVelPIDNoSave(velPID.getDouble("P"), velPID.getDouble("I"), velPID.getDouble("D"));
		this.feedForwardForce = data.getDouble("FeedForwardForce");
	}

	@Override
	protected void saveShared(final CompoundTag data) {
		super.saveShared(data);
		data.putBoolean("AutoAttach", this.autoAttach);
		final BlockPos headPos = this.headPos != null ? this.headPos : this.pendingHeadPos;
		if (headPos != null) {
			data.putIntArray("HeadPos", new int[]{headPos.getX(), headPos.getY(), headPos.getZ()});
			data.putByte("HeadDir", (byte) (this.headDirOppo.ordinal()));
		}
		data.putBoolean("PositionMode", this.positionMode);
		data.putDouble("TargetAngle", this.targetAngle);
		data.putDouble("TargetVelocity", this.targetVelocity);
		final PID posPID = this.getPosPID();
		final CompoundTag posPIDTag = new CompoundTag();
		posPIDTag.putDouble("P", posPID.getKp());
		posPIDTag.putDouble("I", posPID.getKi());
		posPIDTag.putDouble("D", posPID.getKd());
		data.put("PosPID", posPIDTag);
		final PID velPID = this.getVelPID();
		final CompoundTag velPIDTag = new CompoundTag();
		velPIDTag.putDouble("P", velPID.getKp());
		velPIDTag.putDouble("I", velPID.getKi());
		velPIDTag.putDouble("D", velPID.getKd());
		data.put("VelPID", velPIDTag);
		data.putDouble("FeedForwardForce", this.feedForwardForce);
	}

	private double readAngle() {
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final ServerShip ship = ShipUtil.getServerShip(level, this.getBlockPos());
		final ServerShip other = ShipUtil.getServerShip(level, this.headPos);
		final Direction dir = this.getDirection();
		final Vector3dc dirVec = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());
		final Quaterniond relRot = ShipUtil.getShipRelativeRotation(ship, other).mul(this.relOrientation).normalize();
		final double dot = dirVec.dot(relRot.x, relRot.y, relRot.z);
		return MathUtil.normalizeAngle(2 * Math.atan2(dot, relRot.w));
	}

	private double readAngleInPhy(final PhysShip selfShip, final PhysShip otherShip) {
		final Direction dir = this.getDirection();
		final Vector3dc dirVec = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());
		final Quaterniond relRot = ShipUtil.getShipRelativeRotation(selfShip, otherShip).mul(this.relOrientation).normalize();
		final double dot = dirVec.dot(relRot.x, relRot.y, relRot.z);
		return MathUtil.normalizeAngle(2 * Math.atan2(dot, relRot.w));
	}

	@Override
	public boolean attachTo(final BlockPos otherPos) {
		if (this.headPos != null) {
			return false;
		}

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

		this.headPos = otherPos;
		this.headDirOppo = head.getDirection().getOpposite();
		this.setChanged();

		this.relOrientation = new Quaterniond(this.getDirection().getRotation())
			.invert()
			.mul(new Quaterniond(this.headDirOppo.getRotation()));

		final double angle = this.readAngle();
		this.angle = angle;

		final VSAttachmentConstraint attachConstraint1 = new VSAttachmentConstraint(
			selfId,
			otherId,
			ATTACH_COMPLIANCE,
			new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
			new Vector3d(otherPos.getX() + 0.5, otherPos.getY() + 0.5, otherPos.getZ() + 0.5),
			ATTACH_MAX_FORCE,
			0
		);
		final VSConstraint attachConstraint2 = this.createFreeRotationConstraint();
		this.servoInfo = new ServoInfo();
		this.servoInfo.attachConstraint1Id = world.createNewConstraint(attachConstraint1);
		this.servoInfo.attachConstraint2Id = world.createNewConstraint(attachConstraint2);

		return true;
	}

	protected VSConstraint createFreeRotationConstraint() {
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final Quaterniondc baseRot = new Quaterniond(this.getDirection().getRotation()).mul(FREEROT_QUAT);
		final Quaterniond otherRot = new Quaterniond(this.headDirOppo.getRotation()).mul(FREEROT_QUAT);
		return new VSHingeOrientationConstraint(
			ShipUtil.getShipOrDimId(level, this.getBlockPos()),
			ShipUtil.getShipOrDimId(level, this.headPos),
			ATTACH_COMPLIANCE,
			baseRot,
			otherRot,
			ATTACH_MAX_FORCE
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
		this.headDirOppo = null;
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
	protected int[] getConstraints() {
		return null;
	}

	@Override
	protected void rebuildConstraints() {
		final BlockPos headPos = this.pendingHeadPos;
		if (headPos == null) {
			return;
		}
		this.pendingHeadPos = null;
		this.headPos = headPos;

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

		this.relOrientation = new Quaterniond(this.getDirection().getRotation())
			.invert()
			.mul(new Quaterniond(this.headDirOppo.getRotation()));

		final double angle = this.readAngle();
		this.angle = angle;

		final VSAttachmentConstraint attachConstraint1 = new VSAttachmentConstraint(
			selfId,
			otherId,
			ATTACH_COMPLIANCE,
			new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
			new Vector3d(headPos.getX() + 0.5, headPos.getY() + 0.5, headPos.getZ() + 0.5),
			ATTACH_MAX_FORCE,
			0
		);
		final VSConstraint attachConstraint2 = this.createFreeRotationConstraint();
		this.servoInfo = new ServoInfo();
		this.servoInfo.attachConstraint1Id = world.createNewConstraint(attachConstraint1);
		this.servoInfo.attachConstraint2Id = world.createNewConstraint(attachConstraint2);
	}

	@Override
	protected void removeConstriants() {
		if (this.servoInfo == null || this.servoInfo.detached) {
			return;
		}
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld((ServerLevel) (this.getLevel()));
		this.servoInfo.detach(world);
		this.servoInfo = null;
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
			} else if (level.getBlockEntity(this.headPos) instanceof ServoHeadBlockEntity head) {
				if (head.basePos == null) {
					head.basePos = pos;
					head.servoInfo = this.servoInfo;
				}
			} else if (ShipUtil.getShipOrDimId(level, pos) == ShipUtil.getShipOrDimId(level, this.headPos)) {
				this.detach();
			}
		}
		if (!this.isAttached()) {
			this.working = false;
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
		this.working = canWork;

		this.transferHeat(this.heatBuilt.getAndSet(0));
	}

	@Override
	public void physicsTick(final PhysShip ship, final Function<Long, PhysShip> lookup) {
		if (!this.working) {
			return;
		}

		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos headPos = this.headPos;
		if (headPos == null) {
			return;
		}
		final ServerShip otherSShip = VSGameUtilsKt.getShipManagingPos(level, headPos);
		final PhysShip otherShip = otherSShip == null ? null : lookup.apply(otherSShip.getId());

		this.stepServo(ship, otherShip, 1.0 / 60);
	}

	void stepServo(final PhysShip ship, final PhysShip otherShip, final double dt) {
		if (!this.working) {
			return;
		}

		final int positionLoopScale = 1; // TODO: should run position PID slower?
		final boolean shouldRunPos = this.positionMode && (this.physTick % positionLoopScale == 0);
		this.physTick++;

		final Matrix4d transform = ship == null ? new Matrix4d() : new Matrix4d(ship.getTransform().getShipToWorld());
		final Direction dir = this.getDirection();
		final Vector3dc axis = transform.transformDirection(new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ()));
		final double currentAngle = this.readAngleInPhy(ship, otherShip);
		final double currentVelocity = MathUtil.normalizeAngle(currentAngle - this.lastAngle) / dt;
		this.lastAngle = currentAngle;

		final AnglePID posPID = this.posPID;
		final VelocityPID velPID = this.velPID;
		// final GravityFeedForwarder gff = this.gravityFeedForwarder;
		final double fff = this.feedForwardForce;

		double targetVelocity = this.targetVelocity;
		if (shouldRunPos) {
			targetVelocity = posPID.update(currentAngle, this.getTargetAngle(), dt * positionLoopScale);
			this.targetVelocity = targetVelocity;
		}

		double force = velPID.update(currentVelocity, targetVelocity, dt);
		// if (gff.alpha != 0 && Math.abs(force) < ROTATE_MAX_FORCE && Math.abs(currentVelocity) < 0.03 && Math.abs(targetVelocity) < 0.03) {
		// 	gff.feed(currentAngle, force);
		// }

		// final double gfff = gff.alpha == 0 ? 0 : gff.compute(currentAngle);
		force = Math.min(Math.max(force + fff, -ROTATE_MAX_FORCE), ROTATE_MAX_FORCE);

		this.lastTorque = force;
		this.heatBuilt.addAndGet((int) (force / ROTATE_MAX_FORCE * 200));
		final Vector3d torque = axis.mul(force, new Vector3d());

		if (otherShip != null) {
			otherShip.applyInvariantTorque(torque);
		}
		if (ship != null) {
			ship.applyInvariantTorque(torque.negate(new Vector3d()));
		}
	}

	static final class ServoInfo {
		boolean detached = false;
		int attachConstraint1Id, attachConstraint2Id;

		void detach(final ServerShipWorldCore world) {
			world.removeConstraint(this.attachConstraint1Id);
			world.removeConstraint(this.attachConstraint2Id);
			this.detached = true;
		}
	}

	public interface PID {
		double getKp();
		double getKi();
		double getKd();
	}

	private static final class AnglePID implements PID {
		private final double kp;
		private final double ki;
		private final double kd;
		private final double maxOutput;
		private double lastAngle = 0;
		private double integral = 0;

		public AnglePID(final double kp, final double ki, final double kd, final double maxOutput) {
			this.kp = kp;
			this.ki = ki;
			this.kd = kd;
			this.maxOutput = maxOutput;
		}

		@Override
		public final double getKp() {
			return this.kp;
		}

		@Override
		public final double getKi() {
			return this.ki;
		}

		@Override
		public final double getKd() {
			return this.kd;
		}

		public double update(final double currentAngle, final double targetAngle, final double dt) {
			final double error = MathUtil.normalizeAngle(targetAngle - currentAngle);
			final double integral = this.integral + error * dt;
			final double velocity = MathUtil.normalizeAngle(currentAngle - this.lastAngle) / dt;
			double output = this.kp * error + this.ki * integral - this.kd * velocity;
			this.lastAngle = currentAngle;
			if (Math.abs(output) > this.maxOutput) {
				output = Math.signum(output) * this.maxOutput;
			} else {
				this.integral = integral;
			}
			return output;
		}
	}

	private static final class VelocityPID implements PID {
		private final double kp;
		private final double ki;
		private final double kd;
		private final double maxOutput;
		private final double intLimit;
		private double lastVel = 0;
		private double integral = 0;

		public VelocityPID(final double kp, final double ki, final double kd, final double maxOutput) {
			this.kp = kp;
			this.ki = ki;
			this.kd = kd;
			this.maxOutput = maxOutput;
			this.intLimit = maxOutput / Math.max(ki, 1e-6);
		}

		@Override
		public final double getKp() {
			return this.kp;
		}

		@Override
		public final double getKi() {
			return this.ki;
		}

		@Override
		public final double getKd() {
			return this.kd;
		}

		public double update(final double currentVelocity, final double targetVelocity, final double dt) {
			final double error = targetVelocity - currentVelocity;
			final double integral = Math.min(Math.max(this.integral + error * dt, -this.intLimit), this.intLimit);
			double output = this.kp * error + this.ki * integral - this.kd * (currentVelocity - this.lastVel) / dt;
			this.lastVel = currentVelocity;
			if (Math.abs(output) > this.maxOutput) {
				output = Math.signum(output) * this.maxOutput;
			} else {
				this.integral = integral;
			}
			return output;
		}
	}

	// private static final class GravityFeedForwarder {
	// 	private final double alpha;
	// 	private final double alpha0;
	// 	private double sinFeed = 0;
	// 	private double cosFeed = 0;

	// 	public GravityFeedForwarder(final double alpha) {
	// 		this.alpha = alpha;
	// 		this.alpha0 = 1 - alpha;
	// 	}

	// 	public void feed(final double currentAngle, final double torqueFeedback) {
	// 		final double s = Math.sin(currentAngle);
	// 		final double c = Math.cos(currentAngle);
	// 		this.sinFeed = this.alpha0 * this.sinFeed + this.alpha * torqueFeedback * s;
	// 		this.cosFeed = this.alpha0 * this.cosFeed + this.alpha * torqueFeedback * c;
	// 	}

	// 	public double compute(final double angle) {
	// 		return this.sinFeed * Math.sin(angle) + this.cosFeed * Math.cos(angle);
	// 	}
	// }
}
