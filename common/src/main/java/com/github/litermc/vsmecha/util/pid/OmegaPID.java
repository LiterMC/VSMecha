package com.github.litermc.vsmecha.util.pid;

public final class OmegaPID extends PID {
	private final double maxOutput;
	private final double intLimit;
	private double lastVel = 0;

	public OmegaPID(final double kp, final double ki, final double kd, final double maxOutput) {
		super(kp, ki, kd);
		this.maxOutput = maxOutput;
		this.intLimit = maxOutput / Math.max(ki, 1e-6);
	}

	public OmegaPID recreate(final double kp, final double ki, final double kd) {
		final OmegaPID pid = new OmegaPID(kp, ki, kd, this.maxOutput);
		pid.lastVel = this.lastVel;
		return pid;
	}

	public double update(final double currentVelocity, final double targetVelocity, final double dt) {
		final double error = targetVelocity - currentVelocity;
		final double integral = Math.min(Math.max(this.integral + error * dt, -this.intLimit), this.intLimit);
		double output = this.kp * error + this.ki * integral - this.kd * (currentVelocity - this.lastVel) / dt;
		this.lastVel = currentVelocity;
		if (Math.abs(output) > this.maxOutput) {
			final double sig = Math.signum(output);
			output = sig * this.maxOutput;
			if (sig == Math.signum(this.integral - integral)) {
				this.integral = integral;
			}
		} else {
			this.integral = integral;
		}
		return output;
	}
}
