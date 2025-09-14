package com.github.litermc.vsmecha.block.port;

import com.github.litermc.vsmecha.Constants;
import com.github.litermc.vsmecha.block.port.FluidPortBlockEntity;
import com.github.litermc.vsmecha.platform.WrappedFluidStack;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class FluidPortBlockEntityCapabilityProvider implements ICapabilityProvider {
	public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(Constants.MOD_ID, "fluid_port");

	private final FluidPortBlockEntity be;
	private final LazyOptional<IFluidHandler> fluidHandler;

	private FluidPortBlockEntityCapabilityProvider(final FluidPortBlockEntity be) {
		this.be = be;
		this.fluidHandler = LazyOptional.of(() -> new FluidHandler(this.be));
	}

	@Override
	public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) {
			return side == this.be.getDirection() ? LazyOptional.empty() : this.fluidHandler.cast();
		}
		return LazyOptional.empty();
	}

	private void invalidate() {
		this.fluidHandler.invalidate();
	}

	public static void onGatherCapabilities(final AttachCapabilitiesEvent<FluidPortBlockEntity> event) {
		final FluidPortBlockEntityCapabilityProvider provider = new FluidPortBlockEntityCapabilityProvider(event.getObject());
		event.addCapability(CAPABILITY_ID, provider);
		event.addListener(provider::invalidate);
	}

	private static final class FluidHandler implements IFluidHandler {
		private final FluidPortBlockEntity be;

		private FluidHandler(final FluidPortBlockEntity be) {
			this.be = be;
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(final int tank) {
			return FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(final int tank) {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean isFluidValid(final int tank, final FluidStack stack) {
			return true;
		}

		@Override
		public int fill(final FluidStack resource, final FluidAction action) {
			return this.be.pushFluid(new WrappedFluidStack(resource), action.simulate());
		}

		@Override
		public FluidStack drain(final FluidStack resource, final FluidAction action) {
			return FluidStack.EMPTY;
		}

		@Override
		public FluidStack drain(final int maxDrain, final FluidAction action) {
			return FluidStack.EMPTY;
		}
	}
}
