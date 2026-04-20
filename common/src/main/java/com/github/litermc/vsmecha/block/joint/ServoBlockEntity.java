package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.compat.computercraft.ServoPeripheral;
import com.github.litermc.vsmecha.util.MathUtil;
import com.github.litermc.vsmecha.util.pid.AnglePID;
import com.github.litermc.vsmecha.util.pid.OmegaPID;
import com.github.litermc.vsmecha.util.pid.PID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.AxisAngle4d;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.internal.joints.VSJoint;
import org.valkyrienskies.core.internal.joints.VSJointMaxForceTorque;
import org.valkyrienskies.core.internal.joints.VSJointPose;
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint;

public class ServoBlockEntity extends AbstractServoBlockEntity {
	private static final Quaterniondc FREEROT_QUAT = new Quaterniond(new AxisAngle4d(Math.PI / 2, 0, 0, 1));
	private static final double JOINT_COMPLIANCE = 1e-13;
	private static final double JOINT_MAX_FORCE = 1e13;
	private static final VSJointMaxForceTorque JOINT_MAX_FORCE_TORQUE = new VSJointMaxForceTorque((float) JOINT_MAX_FORCE, (float) JOINT_MAX_FORCE);
	// private static final double ROTATE_COMPLIANCE = 0;
	private static final double ROTATE_MAX_FORCE = 1e13;

	private volatile double angle = 0;
	private volatile double targetAngle = 0;
	private volatile double targetVelocity = 0;
	private volatile double lastTorque = 0;
	private volatile AnglePID posPID = new AnglePID(3, 0, 4, 15 * Math.PI / 180);
	private volatile OmegaPID velPID = new OmegaPID(3e5, 1e2, 0, ROTATE_MAX_FORCE);
	private volatile double feedForwardForce = 0;
	// private volatile GravityFeedForwarder gravityFeedForwarder = new GravityFeedForwarder(0.02);

	private double lastAngle = 0;

	public ServoBlockEntity(final BlockEntityType<? extends ServoBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public ServoBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.SERVO.get(), pos, state);
	}

	public double getCurrentAngle() {
		return this.angle;
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

	/**
	 * @return max rotation speed in rad/s
	 */
	public double getMaxRotationSpeed() {
		return this.posPID.getMaxOutput();
	}

	public void setMaxRotationSpeed(double speed) {
		speed = Math.max(Math.abs(speed), 6 * 360 / 180 * Math.PI);
		if (this.posPID.getMaxOutput() == speed) {
			return;
		}
		this.posPID.setMaxOutput(speed);
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

	public void setPosPID(final double p, final double i, final double d) {
		this.posPID = this.posPID.recreate(p, i, d);
		this.setChanged();
	}

	public void setVelPID(final double p, final double i, final double d) {
		this.velPID = this.velPID.recreate(p, i, d);
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

	@Override
	protected boolean canAttachHead(Class<?> headClass) {
		return ServoHeadBlockEntity.class.isAssignableFrom(headClass);
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
	public void load(final CompoundTag data) {
		super.load(data);
		this.targetAngle = MathUtil.normalizeAngle(data.getDouble("TargetAngle"));
		this.targetVelocity = data.getDouble("TargetVelocity");
		this.feedForwardForce = data.getDouble("FeedForwardForce");
		this.posPID = PID.parseTag(data.getCompound("PosPID"), this.posPID::recreate);
		this.velPID = PID.parseTag(data.getCompound("VelPID"), this.velPID::recreate);
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putDouble("TargetAngle", this.targetAngle);
		data.putDouble("TargetVelocity", this.targetVelocity);
		data.putDouble("FeedForwardForce", this.feedForwardForce);
		data.put("PosPID", this.posPID.asTag());
		data.put("VelPID", this.velPID.asTag());
	}

	private double readAngle() {
		final Direction dir = this.getDirection();
		final Vector3dc dirVec = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());
		final Quaterniond relRot = this.readRotation();
		final double dot = dirVec.dot(relRot.x, relRot.y, relRot.z);
		return MathUtil.normalizeAngle(2 * Math.atan2(dot, relRot.w));
	}

	private double readAngleInPhy(final PhysShip selfShip, final PhysShip otherShip) {
		final Direction dir = this.getDirection();
		final Vector3dc dirVec = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());
		final Quaterniond relRot = this.readRotationInPhy(selfShip, otherShip);
		final double dot = dirVec.dot(relRot.x, relRot.y, relRot.z);
		return MathUtil.normalizeAngle(2 * Math.atan2(dot, relRot.w));
	}

	@Override
	protected Object createPeripheral() {
		return new ServoPeripheral(this);
	}

	@Override
	public boolean canConnectPeripheralWire(final Direction dir) {
		return this.getDirection() != dir;
	}

	@Override
	public boolean attachTo(final BlockPos otherPos) {
		if (!super.attachTo(otherPos)) {
			return false;
		}
		this.angle = this.readAngle();
		return true;
	}

	@Override
	protected void rebuildJoints() {
		super.rebuildJoints();
		if (this.isAttached()) {
			this.angle = this.readAngle();
		}
	}

	@Override
	protected VSJoint[] rebuildJointsFor(final long selfId, final long otherId) {
		final BlockPos pos = this.getBlockPos();
		final BlockPos headPos = this.headPos;
		final Quaterniondc baseRot = new Quaterniond(this.getDirection().getRotation()).mul(FREEROT_QUAT);
		final Quaterniondc otherRot = new Quaterniond(this.headDirOppo.getRotation()).mul(FREEROT_QUAT);

		return new VSJoint[]{
			new VSRevoluteJoint(
				selfId,
				new VSJointPose(new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), baseRot),
				otherId,
				new VSJointPose(new Vector3d(headPos.getX() + 0.5, headPos.getY() + 0.5, headPos.getZ() + 0.5), otherRot),
				JOINT_MAX_FORCE_TORQUE,
				JOINT_COMPLIANCE,
				null,
				null,
				null,
				null,
				null
			)
		};
	}

	@Override
	public void stepServo(final PhysShip ship, final PhysShip otherShip, final double dt) {
		final double currentAngle = this.readAngleInPhy(ship, otherShip);
		this.angle = currentAngle;
		if (!this.isWorking()) {
			return;
		}

		final int positionLoopScale = this.getPositionLoopScale();
		final boolean shouldRunPos = this.preTickPositionPID();

		final Matrix4d transform = ship == null ? new Matrix4d() : new Matrix4d(ship.getTransform().getShipToWorld());
		final Direction dir = this.getDirection();
		final Vector3dc axis = transform.transformDirection(new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ()));
		final double currentVelocity = MathUtil.normalizeAngle(currentAngle - this.lastAngle) / dt;
		this.lastAngle = currentAngle;

		// final GravityFeedForwarder gff = this.gravityFeedForwarder;
		final double fff = this.feedForwardForce;

		double targetVelocity = this.targetVelocity;
		if (shouldRunPos) {
			targetVelocity = this.posPID.update(currentAngle, this.getTargetAngle(), dt * positionLoopScale);
			this.targetVelocity = targetVelocity;
		}

		double force = this.velPID.update(currentVelocity, targetVelocity, dt);
		// if (gff.alpha != 0 && Math.abs(force) < ROTATE_MAX_FORCE && Math.abs(currentVelocity) < 0.03 && Math.abs(targetVelocity) < 0.03) {
		// 	gff.feed(currentAngle, force);
		// }

		// final double gfff = gff.alpha == 0 ? 0 : gff.compute(currentAngle);
		force = Math.min(Math.max(force + fff, -ROTATE_MAX_FORCE), ROTATE_MAX_FORCE);

		this.lastTorque = force;
		this.heatBuilt.addAndGet((int) (force / ROTATE_MAX_FORCE * 200));
		final Vector3d torque = axis.mul(force, new Vector3d());

		if (otherShip != null) {
			otherShip.applyWorldTorque(torque);
		}
		if (ship != null) {
			ship.applyWorldTorque(torque.negate(new Vector3d()));
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
