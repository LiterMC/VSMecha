package com.github.litermc.vsmecha.platform;

import net.minecraft.world.level.material.Fluid;

public interface FluidInterface {
	int pushFluid(Stack available, boolean simulate);
	int pullFluid(Stack needs, boolean simulate);

	public interface Stack {
		Fluid getFluid();
		int getAmount();
		Stack copyWithAmount(int amount);
	}
}
