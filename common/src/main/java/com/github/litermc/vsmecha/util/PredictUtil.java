package com.github.litermc.vsmecha.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.Ship;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class PredictUtil {

	private static final int PREDICT_STEPS = 6;
	private static final double[] PREDICT_SCALES = new double[PREDICT_STEPS];
	static {
		for (int i = 0; i < PREDICT_STEPS; i++) {
			PREDICT_SCALES[i] = 1 + (i + 1.0) / 4;
		}
	}
	private static final Map<Long, PredictData> PREDICT_CACHES = new HashMap<>();
	private static final Vector3dc[] CHECK_POINT_OFFSETS = new Vector3dc[]{
		new Vector3d(0, 0, 0),
		new Vector3d(0, 0, 1),
		new Vector3d(0, 1, 0),
		new Vector3d(0, 1, 1),
		new Vector3d(1, 0, 0),
		new Vector3d(0, 0, 1),
		new Vector3d(0, 1, 0),
		new Vector3d(1, 1, 1),
		new Vector3d(0.5, 0.5, 0.5)
	};

	private PredictUtil() {}

	public static void postServerTick() {
		PREDICT_CACHES.values().removeIf((data) -> {
			data.status--;
			return data.status < 0;
		});
	}

	public static PredictData predict(final Ship ship) {
		final PredictData data = PREDICT_CACHES.computeIfAbsent(ship.getId(), (id) -> new PredictData());
		if (data.status == 1) {
			return data;
		}
		data.status = 1;
		final Matrix4dc prevMat = ship.getPrevTickTransform().getShipToWorld();
		final Matrix4dc currentMat = ship.getTransform().getShipToWorld();

		data.predictMats[0].set(currentMat);
		for (int i = 0; i < PREDICT_STEPS; i++) {
			prevMat.lerp(currentMat, PREDICT_SCALES[i], data.predictMats[i + 1]);
		}
		return data;
	}

	public static final class PredictData {
		private int status = 0;
		public final Matrix4d[] predictMats = new Matrix4d[PREDICT_STEPS + 1];

		public PredictData() {
			for (int i = 0; i <= PREDICT_STEPS; i++) {
				this.predictMats[i] = new Matrix4d();
			}
		}

		public void getImpacting(final BlockPos toolBlock, final Collection<Ship> ships, final Map<BlockPos, Double> impacted) {
			final Vector3d
				toolBlockCheckPos = new Vector3d(),
				prevPos = new Vector3d(),
				currPos = new Vector3d(),
				tmp = new Vector3d();
			for (final Vector3dc cp : CHECK_POINT_OFFSETS) {
				cp.add(toolBlock.getX(), toolBlock.getY(), toolBlock.getZ(), toolBlockCheckPos);
				this.predictMats[0].transformPosition(toolBlockCheckPos, currPos);
				for (int i = 1; i <= PREDICT_STEPS; i++) {
					prevPos.set(currPos);
					this.predictMats[i].transformPosition(toolBlockCheckPos, currPos);

					for (final Ship ship : ships) {
						tmp.set(prevPos);
						if (ship != null) {
							ship.getTransform().getWorldToShip().transformPosition(tmp);
						}
						final Vec3 from = new Vec3(tmp.x, tmp.y, tmp.z);
						tmp.set(currPos);
						if (ship != null) {
							ship.getTransform().getWorldToShip().transformPosition(tmp);
						}
						final Vec3 to = new Vec3(tmp.x, tmp.y, tmp.z);
						final Double vel = Double.valueOf(from.distanceTo(to));
						BlockGetter.traverseBlocks(from, to, impacted, (posMap, pos) -> {
							posMap.compute(pos.immutable(), (pos1, oldVel) -> oldVel == null || oldVel < vel ? vel : oldVel);
							return null;
						}, (posMap) -> null);
					}
				}
			}
		}
	}
}
