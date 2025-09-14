package com.github.litermc.vsmecha.platform;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

public record WrappedFluidStack(FluidStack stack) implements FluidInterface.Stack {
	@Override
	public Fluid getFluid() {
		return this.stack.getRawFluid();
	}

	@Override
	public int getAmount() {
		return this.stack.getAmount();
	}

	@Override
	public FluidInterface.Stack copyWithAmount(int amount) {
		return new WrappedFluidStack(new FluidStack(this.stack, amount));
	}
}
