package com.github.litermc.vsmecha.util.pid;

import net.minecraft.nbt.CompoundTag;

public interface PID {
	double getKp();
	double getKi();
	double getKd();

	default CompoundTag asTag() {
		final CompoundTag data = new CompoundTag();
		data.putDouble("P", this.getKp());
		data.putDouble("I", this.getKi());
		data.putDouble("D", this.getKd());
		return data;
	}

	public static <T> T parseTag(final CompoundTag data, final Factory<T> factory) {
		return factory.create(data.getDouble("P"), data.getDouble("I"), data.getDouble("D"));
	}

	@FunctionalInterface
	public interface Factory<T> {
		T create(double p, double i, double d);
	}
}
