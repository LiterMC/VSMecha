package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.compat.computercraft.GimbalServoPeripheral;
import com.github.litermc.vsmecha.util.pid.AnglePID;
import com.github.litermc.vsmecha.util.pid.OmegaPID;
import com.github.litermc.vsmecha.util.pid.PID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.core.internal.joints.VSJoint;
import org.valkyrienskies.core.internal.joints.VSJointMaxForceTorque;
import org.valkyrienskies.core.internal.joints.VSJointPose;
import org.valkyrienskies.core.internal.joints.VSSphericalJoint;

public class GimbalServoBlockEntity extends AbstractServoBlockEntity {
	private static final Quaterniondc ZERO_QUAT = new Quaterniond();
	private static final Vector3dc ZERO_VEC3 = new Vector3d();
	private static final double JOINT_COMPLIANCE = 1e-13;
	private static final double JOINT_MAX_FORCE = 1e13;
	private static final double ROTATE_MAX_FORCE = 1e13;
	private static final VSJointMaxForceTorque JOINT_MAX_FORCE_TORQUE = new VSJointMaxForceTorque((float) JOINT_MAX_FORCE, (float) JOINT_MAX_FORCE);

	private static final double DEFAULT_PITCH_SPEED = Math.toRadians(15);
	private static final double DEFAULT_YAW_SPEED = Math.toRadians(15);
	private static final double DEFAULT_ROLL_SPEED = Math.toRadians(15);

	private volatile Quaterniondc rotation = ZERO_QUAT;
	private volatile Quaterniondc targetRotation = ZERO_QUAT;
	private final Component compX = new Component(new AnglePID(3, 0, 0, DEFAULT_PITCH_SPEED), new OmegaPID(3e5, 1e2, 0, ROTATE_MAX_FORCE));
	private final Component compY = new Component(new AnglePID(3, 0, 0, DEFAULT_YAW_SPEED), new OmegaPID(3e5, 1e2, 0, ROTATE_MAX_FORCE));
	private final Component compZ = new Component(new AnglePID(3, 0, 0, DEFAULT_ROLL_SPEED), new OmegaPID(3e5, 1e2, 0, ROTATE_MAX_FORCE));

	private Quaterniondc lastRotation = ZERO_QUAT;
	private volatile Vector3dc lastOmega = ZERO_VEC3;
	private volatile Vector3dc lastTorque = ZERO_VEC3;

	public GimbalServoBlockEntity(final BlockEntityType<? extends GimbalServoBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public GimbalServoBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.GIMBAL_SERVO.get(), pos, state);
	}

	public Quaterniondc getCurrentRotation() {
		return this.rotation;
	}

	public Quaterniondc getTargetRotation() {
		return this.targetRotation;
	}

	public void setTargetRotation(final Quaterniondc rotation) {
		this.targetRotation = rotation;
		this.setChanged();
	}

	public double getPitchTargetVelocity() {
		return this.compX.targetVelocity;
	}

	public void setPitchTargetVelocity(final double velocity) {
		if (this.compX.targetVelocity == velocity) {
			return;
		}
		this.compX.targetVelocity = velocity;
		this.setChanged();
	}

	/**
	 * @return max rotation speed in rad/s for pitch
	 */
	public double getPitchMaxRotationSpeed() {
		return this.compX.anglePID.getMaxOutput();
	}

	public void setPitchMaxRotationSpeed(double speed) {
		speed = Math.min(Math.abs(speed), Math.toRadians(6 * 360));
		if (this.compX.anglePID.getMaxOutput() == speed) {
			return;
		}
		this.compX.anglePID.setMaxOutput(speed);
		this.setChanged();
	}

	public PID getPitchPosPID() {
		return this.compX.anglePID;
	}

	public void setPitchPosPID(final double p, final double i, final double d) {
		this.compX.anglePID = this.compX.anglePID.recreate(p, i, d);
		this.setChanged();
	}

	public PID getPitchVelPID() {
		return this.compX.omegaPID;
	}

	public void setPitchVelPID(final double p, final double i, final double d) {
		this.compX.omegaPID = this.compX.omegaPID.recreate(p, i, d);
		this.setChanged();
	}

	public double getPitchFeedForwardForce() {
		return this.compX.feedForwardForce;
	}

	public void setPitchFeedForwardForce(final double feedForwardForce) {
		this.compX.feedForwardForce = feedForwardForce;
	}

	public double getYawTargetVelocity() {
		return this.compY.targetVelocity;
	}

	public void setYawTargetVelocity(final double velocity) {
		if (this.compY.targetVelocity == velocity) {
			return;
		}
		this.compY.targetVelocity = velocity;
		this.setChanged();
	}

	/**
	 * @return max rotation speed in rad/s for yaw
	 */
	public double getYawMaxRotationSpeed() {
		return this.compY.anglePID.getMaxOutput();
	}

	public void setYawMaxRotationSpeed(double speed) {
		speed = Math.min(Math.abs(speed), Math.toRadians(6 * 360));
		if (this.compY.anglePID.getMaxOutput() == speed) {
			return;
		}
		this.compY.anglePID.setMaxOutput(speed);
		this.setChanged();
	}

	public PID getYawPosPID() {
		return this.compY.anglePID;
	}

	public void setYawPosPID(final double p, final double i, final double d) {
		this.compY.anglePID = this.compY.anglePID.recreate(p, i, d);
		this.setChanged();
	}

	public PID getYawVelPID() {
		return this.compY.omegaPID;
	}

	public void setYawVelPID(final double p, final double i, final double d) {
		this.compY.omegaPID = this.compY.omegaPID.recreate(p, i, d);
		this.setChanged();
	}

	public double getYawFeedForwardForce() {
		return this.compY.feedForwardForce;
	}

	public void setYawFeedForwardForce(final double feedForwardForce) {
		this.compY.feedForwardForce = feedForwardForce;
	}

	public double getRollTargetVelocity() {
		return this.compZ.targetVelocity;
	}

	public void setRollTargetVelocity(final double velocity) {
		if (this.compZ.targetVelocity == velocity) {
			return;
		}
		this.compZ.targetVelocity = velocity;
		this.setChanged();
	}

	/**
	 * @return max rotation speed in rad/s for roll
	 */
	public double getRollMaxRotationSpeed() {
		return this.compZ.anglePID.getMaxOutput();
	}

	public void setRollMaxRotationSpeed(double speed) {
		speed = Math.min(Math.abs(speed), Math.toRadians(6 * 360));
		if (this.compZ.anglePID.getMaxOutput() == speed) {
			return;
		}
		this.compZ.anglePID.setMaxOutput(speed);
		this.setChanged();
	}

	public PID getRollPosPID() {
		return this.compZ.anglePID;
	}

	public void setRollPosPID(final double p, final double i, final double d) {
		this.compZ.anglePID = this.compZ.anglePID.recreate(p, i, d);
		this.setChanged();
	}

	public PID getRollVelPID() {
		return this.compZ.omegaPID;
	}

	public void setRollVelPID(final double p, final double i, final double d) {
		this.compZ.omegaPID = this.compZ.omegaPID.recreate(p, i, d);
		this.setChanged();
	}

	public double getRollFeedForwardForce() {
		return this.compZ.feedForwardForce;
	}

	public void setRollFeedForwardForce(final double feedForwardForce) {
		this.compZ.feedForwardForce = feedForwardForce;
	}

	public Vector3dc getLastOmega() {
		return this.lastOmega;
	}

	public Vector3dc getLastTorque() {
		return this.lastTorque;
	}

	@Override
	protected boolean canAttachHead(Class<?> headClass) {
		return GimbalServoHeadBlockEntity.class.isAssignableFrom(headClass);
	}

	@Override
	public int getEnergyConsumption() {
		return 0;//2500;
	}

	@Override
	public int getMaxHeatCapacity() {
		return 26000;
	}

	@Override
	public int getDangerousHeatLimit() {
		return 23000;
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.compX.fromTag(data.getCompound("CompX"));
		this.compY.fromTag(data.getCompound("CompY"));
		this.compZ.fromTag(data.getCompound("CompZ"));
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.put("CompX", this.compX.asTag());
		data.put("CompY", this.compY.asTag());
		data.put("CompZ", this.compZ.asTag());
	}

	@Override
	protected Object createPeripheral() {
		return new GimbalServoPeripheral(this);
	}

	@Override
	public boolean canConnectPeripheralWire(final Direction dir) {
		return this.getDirection().getOpposite() == dir;
	}

	@Override
	public boolean attachTo(final BlockPos otherPos) {
		if (!super.attachTo(otherPos)) {
			return false;
		}
		this.rotation = this.readRotation();
		return true;
	}

	@Override
	protected void tryRebuildJoints() {
		super.tryRebuildJoints();
		if (this.isAttached()) {
			this.rotation = this.readRotation();
		}
	}

	@Override
	protected VSJoint[] rebuildJointsFor(final long selfId, final long otherId) {
		final BlockPos pos = this.getBlockPos();
		final BlockPos headPos = this.headPos;
		return new VSJoint[]{
			new VSSphericalJoint(
				selfId,
				new VSJointPose(new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), new Quaterniond()),
				otherId,
				new VSJointPose(new Vector3d(headPos.getX() + 0.5, headPos.getY() + 0.5, headPos.getZ() + 0.5), new Quaterniond()),
				JOINT_MAX_FORCE_TORQUE,
				JOINT_COMPLIANCE,
				null
			)
		};
	}

	@Override
	public void stepServo(final PhysLevel world, final PhysShip ship, final PhysShip otherShip, final double dt) {
		final Quaterniond currentRotation = this.readRotationInPhy(ship, otherShip);
		this.rotation = currentRotation;
		if (!this.isWorking()) {
			return;
		}

		final int positionLoopScale = this.getPositionLoopScale();
		final boolean shouldRunPos = this.preTickPositionPID();

		final Vector3d omega = getQuaternionError(currentRotation, this.lastRotation).div(dt);
		this.lastRotation = currentRotation;
		this.lastOmega = omega;
		final Vector3d error = getQuaternionError(currentRotation, this.targetRotation);

		Vector3dc torque = new Vector3d(
			this.compX.step(error.x, omega.x, positionLoopScale, shouldRunPos, dt),
			this.compY.step(error.y, omega.y, positionLoopScale, shouldRunPos, dt),
			this.compZ.step(error.z, omega.z, positionLoopScale, shouldRunPos, dt)
		);
		this.lastTorque = torque;
		this.heatBuilt.addAndGet((int) ((torque.x() + torque.y() + torque.z()) / ROTATE_MAX_FORCE * 200));

		if (otherShip != null) {
			torque = otherShip.getTransform().getShipToWorld().transformDirection(torque, new Vector3d());
			otherShip.applyWorldTorque(torque);
		}
		if (ship != null) {
			ship.applyWorldTorque(torque.negate(new Vector3d()));
		}
	}

	private Vector3d getQuaternionError(final Quaterniondc current, final Quaterniondc target) {
		final Quaterniond error = new Quaterniond(current).conjugate().mul(target);
		if (error.w < 0.0) {
			error.mul(-1);
		}
		error.normalize();
		final double angle = 2 * Math.acos(error.w);
		final double f = Math.sqrt(1 - error.w * error.w);
		if (f < 1e-6) {
			return new Vector3d(error.x, error.y, error.z).mul(2);
		}
		return new Vector3d(error.x, error.y, error.z).mul(angle / f);
	}

	private static final class Component {
		private volatile AnglePID anglePID;
		private volatile OmegaPID omegaPID;
		private volatile double targetVelocity = 0;
		private volatile double feedForwardForce = 0;

		private Component(final AnglePID anglePID, final OmegaPID omegaPID) {
			this.anglePID = anglePID;
			this.omegaPID = omegaPID;
		}

		private double step(
			final double error,
			final double velocity,
			final int positionLoopScale,
			final boolean runPositionLoop,
			final double dt
		) {
			double targetVelocity = this.targetVelocity;
			if (runPositionLoop) {
				targetVelocity = this.anglePID.update(-error, velocity, dt * positionLoopScale);
				this.targetVelocity = targetVelocity;
			}
			final double force = this.omegaPID.update(velocity, targetVelocity, dt);
			return Math.min(Math.max(force + this.feedForwardForce, -ROTATE_MAX_FORCE), ROTATE_MAX_FORCE);
		}

		private CompoundTag asTag() {
			final CompoundTag data = new CompoundTag();
			data.put("PosPID", this.anglePID.asTag());
			data.put("VelPID", this.omegaPID.asTag());
			data.putDouble("TargetVelocity", this.targetVelocity);
			data.putDouble("FeedForwardForce", this.feedForwardForce);
			return data;
		}

		private void fromTag(final CompoundTag data) {
			this.anglePID = PID.parseTag(data.getCompound("PosPID"), this.anglePID::recreate);
			this.omegaPID = PID.parseTag(data.getCompound("VelPID"), this.omegaPID::recreate);
			this.targetVelocity = data.getDouble("TargetVelocity");
			this.feedForwardForce = data.getDouble("FeedForwardForce");
		}
	}
}
