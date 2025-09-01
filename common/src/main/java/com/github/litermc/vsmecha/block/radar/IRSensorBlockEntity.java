package com.github.litermc.vsmecha.block.radar;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.compat.computercraft.radar.IRSensorPeripheral;
import com.github.litermc.vsmecha.util.BlockSourceClipContext;
import com.github.litermc.vsmecha.util.MathUtil;
import com.github.litermc.vsmecha.util.ShipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IRSensorBlockEntity extends RadarBlockEntity {
	private static final double ERROR = 0.005;
	private static final double ANGLE_ERROR = Math.toRadians(0.5);
	private static final double OPT_FACTOR = 1.14;
	private static final EntityTypeTest<Entity, LivingEntity> LIVINGENTITY_TYPE = EntityTypeTest.forClass(LivingEntity.class);

	protected IRSensorBlockEntity(final BlockEntityType<? extends IRSensorBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public IRSensorBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.IR_SENSOR.get(), pos, state);
	}

	@Override
	public int getMaxScanRadius() {
		return 16 * 16;
	}

	public int getOptimalScanRadius(final double size) {
		final Level level = this.getLevel();
		final double r = 16 * 14 - level.getRainLevel(1) * 16 * 4 - level.getThunderLevel(1) * 16 * 8;
		return Math.min((int) (r * size / 2), this.getMaxScanRadius());
	}

	@Override
	public int getEnergyConsumption() {
		return 100;
	}

	@Override
	public int getScanTime() {
		return 2;
	}

	@Override
	public int getMaxEnergyStorage() {
		return 1000;
	}

	@Override
	public int getEnergyInputLimit() {
		return this.getMaxEnergyStorage();
	}

	@Override
	protected void finalizeScan(final List<ScanResult> results) {
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos pos = this.getBlockPos();
		final ServerShip ship = ShipUtil.getServerShip(level, pos);

		final double radius = this.getScanRadius();
		final double height = Math.min(radius, 128);

		final Vec3 scanCenter = pos.getCenter();
		final Vec3 scanCenterWorld = ship == null ? scanCenter : VSGameUtilsKt.toWorldCoordinates(ship, scanCenter);

		final AABBd scanBox = new AABBd(
			scanCenter.x - radius, scanCenter.y - height, scanCenter.z - radius,
			scanCenter.x + radius, scanCenter.y + height, scanCenter.z + radius
		);
		final Matrix4d transform = new Matrix4d().set(this.getOrientation().top().getRotation());
		if (ship != null) {
			ship.getTransform().getShipToWorld().mul(transform, transform);
		}
		final Matrix4dc transformInv = transform.invert(new Matrix4d());
		final AABBd scanBoxWorld = scanBox.transform(transform, new AABBd());
		final Vector3dc topVec = transform.transformDirection(new Vector3d(0, 1, 0));

		final List<LivingEntity> entities = new ArrayList<>();
		final AABBd tmpBB = new AABBd();
		level.getEntities(
			LIVINGENTITY_TYPE,
			new AABB(scanBoxWorld.minX, scanBoxWorld.minY, scanBoxWorld.minZ, scanBoxWorld.maxX, scanBoxWorld.maxY, scanBoxWorld.maxZ),
			(e) -> {
				if (!e.canBeSeenByAnyone()) {
					return false;
				}
				final double size = e.getBbWidth() * e.getBbHeight();
				if (size < 1e-4) {
					return false;
				}
				final AABB bb = e.getBoundingBox();
				final double dist = closestDist(bb, scanCenterWorld);
				if (dist > radius || dist > this.getOptimalScanRadius(size) * OPT_FACTOR) {
					return false;
				}
				if (
					!tmpBB
						.setMin(bb.minX, bb.minY, bb.minZ)
						.setMax(bb.maxX, bb.maxY, bb.maxZ)
						.transform(transformInv)
						.intersectsAABB(scanBox)
				) {
					return false;
				}
				return this.checkEntity(e, scanCenterWorld, topVec);
			},
			entities,
			128
		);

		for (final LivingEntity entity : entities) {
			results.add(this.scanResultFromEntity(entity, scanCenterWorld, transformInv));
		}

		// TODO: scan ships
	}

	@Override
	protected Object createPeripheral() {
		return new IRSensorPeripheral(this);
	}

	protected boolean checkEntity(final LivingEntity entity, final Vec3 scanCenter, final Vector3dc topVec) {
		final Vec3 pos = entity.position();
		final Vector3d entityDir = new Vector3d(pos.x - scanCenter.x, pos.y - scanCenter.y, pos.z - scanCenter.z);
		if (Math.abs(topVec.angle(entityDir)) < Math.PI / 4) {
			// not in sight
			return false;
		}
		if (
			this.getLevel().clip(
				new BlockSourceClipContext(scanCenter, pos, ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, this.getBlockPos())
			)
				.getType() != HitResult.Type.MISS
		) {
			return false;
		}
		return true;
	}

	private ScanResultWithSize scanResultFromEntity(final LivingEntity entity, final Vec3 scanCenter, final Matrix4dc transform) {
		final Vec3 pos = entity.position();
		final Vector3d relPos = new Vector3d(pos.x - scanCenter.x, pos.y - scanCenter.y, pos.z - scanCenter.z);
		final double distance = relPos.length();

		transform.transformDirection(relPos.normalize());
		final double xRot = Math.asin(-relPos.y);
		final double yRot = Math.atan2(relPos.x, relPos.z);

		final double width = entity.getBbWidth();
		final double height = entity.getBbHeight();
		return this.createScanResultWithError(distance, xRot, yRot, ScanResultWithType.TYPE_ENTITY, width, height);
	}

	private ScanResultWithSize createScanResultWithError(
		double distance,
		double xRot,
		double yRot,
		final String type,
		double width,
		double height
	) {
		final double optRange = this.getOptimalScanRadius(width * height);
		final double optDiff = distance - optRange;
		if (optDiff > 0) {
			distance = applyError(distance, ERROR);
			xRot += MathUtil.normalizeAngle(generateRandom(Double.hashCode(xRot), -ANGLE_ERROR, ANGLE_ERROR));
			yRot += MathUtil.normalizeAngle(generateRandom(Double.hashCode(yRot), -ANGLE_ERROR, ANGLE_ERROR));
			final double minSize = Math.min(width, height);
			width = applyError(minSize, ERROR);
			height = applyError(minSize + 1e-6, ERROR);
		}
		return new ScanResultWithSize(distance, xRot, yRot, type, width, height);
	}

	public static class ScanResultWithSize extends ScanResultWithType {
		private final double width;
		private final double height;

		public ScanResultWithSize(
			final double distance,
			final double xRot,
			final double yRot,
			final String type,
			final double width,
			final double height
		) {
			super(distance, xRot, yRot, type);
			this.width = width;
			this.height = height;
		}

		public final double getWidth() {
			return this.width;
		}

		public final double getHeight() {
			return this.height;
		}

		public void saveAsJSON(final Map<String, Object> data) {
			super.saveAsJSON(data);
			data.put("width", this.width);
			data.put("height", this.height);
		}
	}
}
