package com.github.litermc.vsmecha.util.pid;

import net.minecraft.nbt.CompoundTag;

public abstract class PID {
	protected final double kp;
	protected final double ki;
	protected final double kd;
	protected double integral = 0;

	protected PID(final double kp, final double ki, final double kd) {
		this.kp = kp;
		this.ki = ki;
		this.kd = kd;
	}

	public final double getKp() {
		return this.kp;
	}

	public final double getKi() {
		return this.ki;
	}

	public final double getKd() {
		return this.kd;
	}

	public CompoundTag asTag() {
		final CompoundTag data = new CompoundTag();
		data.putDouble("P", this.kp);
		data.putDouble("I", this.ki);
		data.putDouble("D", this.kd);
		data.putDouble("Integral", this.integral);
		return data;
	}

	public void parseAdditional(final CompoundTag data) {
		this.integral = data.getDouble("Integral");
	}

	public static <T extends PID> T parseTag(final CompoundTag data, final Factory<T> factory) {
		final T pid = factory.create(data.getDouble("P"), data.getDouble("I"), data.getDouble("D"));
		pid.parseAdditional(data);
		return pid;
	}

	@FunctionalInterface
	public interface Factory<T extends PID> {
		T create(double p, double i, double d);
	}
}
