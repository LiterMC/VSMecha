package com.github.litermc.vsmecha.block.port;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.compat.computercraft.port.FluidPortPeripheral;
import com.github.litermc.vsmecha.platform.FluidInterface;
import com.github.litermc.vsmecha.platform.PlatformHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class FluidPortBlockEntity extends AbstractPortBlockEntity<FluidInterface.Stack> {
	public FluidPortBlockEntity(final BlockEntityType<? extends FluidPortBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public FluidPortBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.FLUID_PORT.get(), pos, state);
	}

	@Override
	public Object getPortType() {
		return FluidPortBlockEntity.class;
	}

	@Override
	protected Object createPeripheral() {
		return new FluidPortPeripheral(this);
	}

	@Override
	protected void onPortObjectOutput(final Context<FluidInterface.Stack> context) {
		if (context.getAmount() <= 0) {
			return;
		}
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos outPos = this.getBlockPos().relative(this.getDirection());
		final FluidInterface fi = PlatformHelper.get().getFluidInterface(level, outPos, this.getDirection().getOpposite());
		if (fi == null) {
			return;
		}
		final int pushed = fi.pushFluid(context.getObject().copyWithAmount(context.getAmount()), context.isSimulated());
		context.consume(pushed);
	}

	public int pushFluid(final FluidInterface.Stack stack, final boolean simulate) {
		final int amount = stack.getAmount();
		if (amount <= 0) {
			return 0;
		}
		final Context<FluidInterface.Stack> context = new Context<>(stack, amount, simulate);
		this.onPortObjectInput(context);
		return amount - context.getAmount();
	}
}
