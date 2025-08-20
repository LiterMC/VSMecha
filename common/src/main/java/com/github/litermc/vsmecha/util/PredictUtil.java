package com.github.litermc.vsmecha.util;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.shape.IToolShape;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.Ship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class PredictUtil {

	private static final int PREDICT_STEPS = 6;
	private static final double[] PREDICT_SCALES = new double[PREDICT_STEPS];
	static {
		for (int i = 0; i < PREDICT_STEPS; i++) {
			PREDICT_SCALES[i] = 1 + (i + 1.0) / 2;
		}
	}
	private static final Map<Long, PredictData> PREDICT_CACHES = new HashMap<>();
	private static final Vector3dc[] CHECK_POINT_OFFSETS = new Vector3dc[]{
		new Vector3d(0.5, 0.5, 0.5),
		new Vector3d(0, 0, 0),
		new Vector3d(0, 0, 1),
		new Vector3d(0, 1, 0),
		new Vector3d(0, 1, 1),
		new Vector3d(1, 0, 0),
		new Vector3d(0, 0, 1),
		new Vector3d(0, 1, 0),
		new Vector3d(1, 1, 1)
	};
	private static final Predicate<Entity> ENTITY_ATTACKABLE =
		EntitySelector.NO_CREATIVE_OR_SPECTATOR
			.and(EntitySelector.ENTITY_STILL_ALIVE)
			.and(Entity::isAttackable);

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

		public void getImpacting(
			final ServerLevel level,
			final Ship opShip,
			final BlockPos toolBlock,
			final Collection<Ship> ships, 
			final Predicate<Entity> entityFilter,
			final Map<BlockPos, BlockImpactData> impactedBlocks,
			final Map<Entity, EntityImpactData> impactedEntities
		) {
			final Vector3d
				toolBlockCheckPos = new Vector3d(),
				prevPos = new Vector3d(),
				currPos = new Vector3d(),
				tmp = new Vector3d();

			final List<IToolShape> shapes = new ArrayList<>();
			CHECK_POINT_OFFSETS[0].add(toolBlock.getX(), toolBlock.getY(), toolBlock.getZ(), toolBlockCheckPos);
			this.predictMats[0].transformPosition(toolBlockCheckPos, prevPos);
			this.predictMats[1].transformPosition(toolBlockCheckPos, currPos);
			opShip.getTransform().getWorldToShip().transformDirection(prevPos.sub(currPos, tmp).normalize());
			for (final IToolShape shape : VSMechaRegistry.TOOL_SHAPES) {
				if (shape.test(level, toolBlockCheckPos, tmp)) {
					shapes.add(shape);
				}
			}

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
						final double vel = from.distanceTo(to) / Math.max(i - 2, 1);
						BlockGetter.traverseBlocks(from, to, impactedBlocks, (posMap, pos) -> {
							final BlockImpactData data = posMap.computeIfAbsent(pos.immutable(), (pos1) -> new BlockImpactData(level.getBlockState(pos1)));
							if (data.velocity < vel) {
								data.velocity = vel;
							}
							if (!data.hasCorrectToolForDrops) {
								if (shapes.stream().anyMatch((s) -> s.isCorrectToolForDrops(data.state))) {
									data.hasCorrectToolForDrops = true;
								}
							}
							return null;
						}, (posMap) -> null);
						final List<Entity> entities = level.getEntities((Entity) null, new AABB(from, to), ENTITY_ATTACKABLE);
						for (final Entity entity : entities) {
							if (!entityFilter.test(entity)) {
								continue;
							}
							final Vec3 pos = entity.getBoundingBox().clip(from, to).orElse(null);
							if (pos == null) {
								continue;
							}
							final float damageAmplifier = (float) (shapes.stream().mapToDouble((s) -> s.damageAmplifier(entity)).max().orElse(1));
							final EntityImpactData data = impactedEntities.computeIfAbsent(entity, (entity1) -> new EntityImpactData(damageAmplifier));
							if (data.velocity < vel) {
								data.velocity = vel;
							}
							if (data.damageAmplifier < damageAmplifier) {
								data.damageAmplifier = damageAmplifier;
							}
						}
					}
				}
			}
		}
	}

	public static final class BlockImpactData {
		public final BlockState state;
		public double velocity = 0;
		public boolean hasCorrectToolForDrops;

		public BlockImpactData(final BlockState state) {
			this.state = state;
			this.hasCorrectToolForDrops = !state.requiresCorrectToolForDrops();
		}
	}

	public static final class EntityImpactData {
		public double velocity = 0;
		public float damageAmplifier;

		public EntityImpactData(final float damageAmplifier) {
			this.damageAmplifier = damageAmplifier;
		}
	}
}
