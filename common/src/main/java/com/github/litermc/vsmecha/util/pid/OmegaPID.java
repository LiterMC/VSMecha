package com.github.litermc.vsmecha.util.pid;

public final class OmegaPID implements PID {
	private final double kp;
	private final double ki;
	private final double kd;
	private final double maxOutput;
	private final double intLimit;
	private double lastVel = 0;
	private double integral = 0;

	public OmegaPID(final double kp, final double ki, final double kd, final double maxOutput) {
		this.kp = kp;
		this.ki = ki;
		this.kd = kd;
		this.maxOutput = maxOutput;
		this.intLimit = maxOutput / Math.max(ki, 1e-6);
	}

	@Override
	public double getKp() {
		return this.kp;
	}

	@Override
	public double getKi() {
		return this.ki;
	}

	@Override
	public double getKd() {
		return this.kd;
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
			output = Math.signum(output) * this.maxOutput;
		} else {
			this.integral = integral;
		}
		return output;
	}
}
