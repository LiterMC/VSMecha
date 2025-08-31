package com.github.litermc.vsmecha.util.bvh;

import java.util.List;
import java.util.concurrent.atomic.DoubleAdder;

public abstract class BVH<B extends BVH.Bound> {
	/**
	 * @param bound The rough bound to test
	 * @return {@code true}  if the bound is good and need test sub-boundings,
	 *         {@code false} if the bound does not match requirement and should be dropped,
	 *         {@code null}  if the bound does not match requirement but should test sub-boundings if possible.
	 */
	public abstract Boolean roughTest(B bound);

	/**
	 * @param bound outer bounding
	 * @return sub-bounds for the bound, or {@code null} if no sub-tests are needed.
	 */
	public abstract List<B> generateSubBounds(B bound);

	public double getCoverageSize(final B bound) {
		final DoubleAdder coverage = new DoubleAdder();
		this.getCoverageSize0(bound, coverage);
		return coverage.sum();
	}

	private void getCoverageSize0(final B bound, final DoubleAdder coverage) {
		final Boolean res = this.roughTest(bound);
		if (res == Boolean.FALSE) {
			return;
		}
		final List<B> bounds = this.generateSubBounds(bound);
		if (bounds == null) {
			if (res == Boolean.TRUE) {
				coverage.add(bound.getSize());
			}
			return;
		}
		for (final B b : bounds) {
			this.getCoverageSize0(b, coverage);
		}
	}

	public interface Bound {
		double getSize();
	}
}
