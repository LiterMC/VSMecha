package com.github.litermc.vsmecha.util.pid;

public final class AnglePID implements PID {
	private final double kp;
	private final double ki;
	private final double kd;
	private double integral = 0;
	private volatile double maxOutput;

	public AnglePID(final double kp, final double ki, final double kd, final double maxOutput) {
		this.kp = kp;
		this.ki = ki;
		this.kd = kd;
		this.maxOutput = maxOutput;
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

	public double getMaxOutput() {
		return this.maxOutput;
	}

	public void setMaxOutput(final double maxOutput) {
		this.maxOutput = maxOutput;
	}

	public AnglePID recreate(final double kp, final double ki, final double kd) {
		return new AnglePID(kp, ki, kd, this.maxOutput);
	}

	public double update(final double error, final double velocity, final double dt) {
		final double integral = this.integral + error * dt;
		double output = this.kp * error + this.ki * integral - this.kd * velocity;
		final double maxOutput = this.maxOutput;
		if (Math.abs(output) > maxOutput) {
			output = Math.signum(output) * maxOutput;
		} else {
			this.integral = integral;
		}
		return output;
	}
}
