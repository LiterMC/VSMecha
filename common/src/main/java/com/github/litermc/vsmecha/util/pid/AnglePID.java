package com.github.litermc.vsmecha.util.pid;

import net.minecraft.nbt.CompoundTag;

public final class AnglePID extends PID {
	private volatile double maxOutput;

	public AnglePID(final double kp, final double ki, final double kd, final double maxOutput) {
		super(kp, ki, kd);
		this.maxOutput = maxOutput;
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

	@Override
	public CompoundTag asTag() {
		final CompoundTag data = super.asTag();
		data.putDouble("MaxOutput", this.maxOutput);
		return data;
	}

	@Override
	public void parseAdditional(final CompoundTag data) {
		super.parseAdditional(data);
		this.maxOutput = data.getDouble("MaxOutput");
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
