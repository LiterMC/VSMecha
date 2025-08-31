package com.github.litermc.vsmecha.util.bvh;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3d;

/**
 * Axis aligned clip BVH
 */
public abstract class ClipSurfaceBVH extends SurfaceBVH {
	private final double maxSizeSqr;
	private final BlockGetter level;
	private final Direction.Axis axis;
	private final double ox;
	private final double oy;
	private final double oz;
	private final double maxPrependDist;
	private final double extendRange;

	public ClipSurfaceBVH(
		final double minSize,
		final double maxSize,
		final BlockGetter level,
		final Direction.Axis axis,
		final Vec3 origin,
		final double maxPrependDist,
		final double extendRange
	) {
		super(minSize);
		this.maxSizeSqr = maxSize * maxSize;
		this.level = level;
		this.axis = axis;
		this.ox = origin.x;
		this.oy = origin.y;
		this.oz = origin.z;
		this.maxPrependDist = maxPrependDist;
		this.extendRange = extendRange;
	}

	public abstract boolean testBlock(final BlockState state, final BlockGetter level, final BlockPos pos);

	public abstract boolean testFluid(final FluidState state, final BlockGetter level, final BlockPos pos);

	public abstract Boolean testHitResult(final BlockHitResult hitResult);

	@Override
	public Boolean roughTest(final SurfaceBound bound) {
		if (bound.getSize() > this.maxSizeSqr) {
			return true;
		}
		final Vector3d start = new Vector3d(this.ox, this.oy, this.oz);
		final Vector3d end = this.getCenterPoint(bound);
		final double dist = start.distance(end);
		if (dist > this.maxPrependDist) {
			start.sub(end).normalize(this.maxPrependDist).add(end);
		}
		end.sub(this.ox, this.oy, this.oz).normalize(dist + this.extendRange).add(this.ox, this.oy, this.oz);

		return this.testHitResult(
			this.level.clip(
				this.new AdvancedClipContext(new Vec3(start.x, start.y, start.z), new Vec3(end.x, end.y, end.z))
			)
		);
	}

	private Vector3d getCenterPoint(final SurfaceBound bound) {
		final double x = (bound.x0 + bound.x1) / 2;
		final double y = (bound.y0 + bound.y1) / 2;
		final double z = bound.z;
		return switch (axis) {
			case X -> new Vector3d(z, y, x);
			case Y -> new Vector3d(x, z, y);
			case Z -> new Vector3d(x, y, z);
		};
	}

	private final class AdvancedClipContext extends ClipContext {
		private AdvancedClipContext(final Vec3 from, final Vec3 to) {
			super(from, to, null, null, null);
		}

		@Override
		public VoxelShape getBlockShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
			return ClipSurfaceBVH.this.testBlock(state, level, pos) ? state.getCollisionShape(level, pos) : Shapes.empty();
		}

		@Override
		public VoxelShape getFluidShape(final FluidState state, final BlockGetter level, final BlockPos pos) {
			return !state.isEmpty() && ClipSurfaceBVH.this.testFluid(state, level, pos) ? state.getShape(level, pos) : Shapes.empty();
		}
	}
}
