package com.github.litermc.vsmecha.util;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class MathUtil {
	public static final double PI2 = Math.PI * 2;

	/**
	 * @return normalized angle in range of (-{@link Math.PI}, {@link Math.PI}]
	 */
	public static final double normalizeAngle(double angle) {
		angle = (angle % PI2 + PI2) % PI2;
		return angle > Math.PI ? angle - PI2 : angle;
	}

	public static final double lerpAngle(final double a, final double b, final double alpha) {
		return normalizeAngle(a + normalizeAngle(b - a) * alpha);
	}

	public static Vector3d[] generatePlaneVectors(final Vector3dc plane, int iter) {
		final Vector3d p1 = new Vector3d().orthogonalizeUnit(plane);
		final Vector3d p2 = plane.cross(p1, new Vector3d()).normalize();

		final int sectionSize = 1 << iter;
		final int size = sectionSize * 4;
		final Vector3d[] outputs = new Vector3d[size];
		outputs[0 * sectionSize] = p1;
		outputs[1 * sectionSize] = p2;
		outputs[2 * sectionSize] = p1.negate(new Vector3d());
		outputs[3 * sectionSize] = p2.negate(new Vector3d());

		while (iter-- > 0) {
			final int ss = 1 << iter;
			final int n = ss << 1;
			outputs[size - ss] = outputs[0].add(outputs[size - n], new Vector3d()).normalize();
			for (int i = 0; i + n < size; i += n) {
				outputs[i + ss] = outputs[i].add(outputs[i + n], new Vector3d()).normalize();
			}
		}

		return outputs;
	}

	public static Vector3d[] generatePlaneAngleVectors(final Vector3dc plane, final int iter, final double radians) {
		return planeVectorsToAngled(plane, radians, generatePlaneVectors(plane, iter));
	}

	public static Vector3d[] planeVectorsToAngled(final Vector3dc plane, final double radians, final Vector3d[] vectors) {
		final Vector3d proj = plane.mul(Math.cos(radians), new Vector3d());
		for (int i = 0; i < vectors.length; i++) {
			vectors[i].mul(Math.sin(radians)).add(proj);
		}
		return vectors;
	}

	private MathUtil() {}
}
