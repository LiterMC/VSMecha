package com.github.litermc.vsmecha.platform;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.level.material.Fluid;

public record WrappedFluidStack(FluidVariant fluid, int amount) implements FluidInterface.Stack {
	@Override
	public Fluid getFluid() {
		return this.fluid.getFluid();
	}

	@Override
	public int getAmount() {
		return this.amount;
	}

	@Override
	public FluidInterface.Stack copyWithAmount(int amount) {
		return new WrappedFluidStack(this.fluid, amount);
	}
}
