package com.github.litermc.vsmecha.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockSourceClipContext extends ClipContext {
	public final BlockPos source;

	public BlockSourceClipContext(
		final Vec3 from,
		final Vec3 to,
		final ClipContext.Block block,
		final ClipContext.Fluid fluid,
		final BlockPos source
	) {
		super(from, to, block, fluid, null);
		this.source = source;
	}

	@Override
	public VoxelShape getBlockShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
		return pos.equals(this.source) ? Shapes.empty() : super.getBlockShape(state, level, pos);
	}

	@Override
	public VoxelShape getFluidShape(final FluidState state, final BlockGetter level, final BlockPos pos) {
		return pos.equals(this.source) ? Shapes.empty() : super.getFluidShape(state, level, pos);
	}
}
