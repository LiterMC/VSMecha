package com.github.litermc.vsmecha.util.bvh;

import java.util.List;

public abstract class SurfaceBVH extends BVH<SurfaceBVH.SurfaceBound> {
	private final double minSize;

	protected SurfaceBVH(final double minSize) {
		this.minSize = minSize;
	}

	public List<SurfaceBound> generateSubBounds(final SurfaceBound bound) {
		final double
			x0 = bound.x0,
			y0 = bound.y0,
			x1 = bound.x1,
			y1 = bound.y1;
		final double xm = (x0 + x1) / 2;
		final double ym = (y0 + y1) / 2;
		final double z = bound.z;
		final boolean splitX = x0 + minSize < x1;
		final boolean splitY = y0 + minSize < y1;
		if (splitX) {
			if (splitY) {
				return List.of(
					new SurfaceBound(x0, y0, xm, ym, z),
					new SurfaceBound(x0, ym, xm, y1, z),
					new SurfaceBound(xm, y0, x1, ym, z),
					new SurfaceBound(xm, ym, x1, y1, z)
				);
			}
			return List.of(
				new SurfaceBound(x0, y0, xm, y1, z),
				new SurfaceBound(xm, y0, x1, y1, z)
			);
		}
		if (splitY) {
			return List.of(
				new SurfaceBound(x0, y0, x1, ym, z),
				new SurfaceBound(x0, ym, x1, y1, z)
			);
		}
		return null;
	}

	public static class SurfaceBound implements BVH.Bound {
		public final double x0, y0, x1, y1;
		public final double z;

		public SurfaceBound(final double x0, final double y0, final double x1, final double y1, final double z) {
			this.x0 = x0;
			this.y0 = y0;
			this.x1 = x1;
			this.y1 = y1;
			this.z = z;
		}

		@Override
		public double getSize() {
			return (this.x1 - this.x0) * (this.y1 - this.y0);
		}
	}
}
