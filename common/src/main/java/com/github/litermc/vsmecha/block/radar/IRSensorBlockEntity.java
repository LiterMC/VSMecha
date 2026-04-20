package com.github.litermc.vsmecha.block.radar;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.attachment.ShipNetworkAttachment;
import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.block.radar.result.ScanResult;
import com.github.litermc.vsmecha.block.radar.result.ScanSizeInfo;
import com.github.litermc.vsmecha.block.radar.result.ScanTypeInfo;
import com.github.litermc.vsmecha.compat.computercraft.radar.IRSensorPeripheral;
import com.github.litermc.vsmecha.entity.SmokeEntity;
import com.github.litermc.vsmecha.util.BlockSourceClipContext;
import com.github.litermc.vsmecha.util.MathUtil;
import com.github.litermc.vsmecha.util.RayCastUtil;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.List;

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
		final VsiServerShipWorld world = VSGameUtilsKt.getShipObjectWorld(level);
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
		final Matrix4d transform = new Matrix4d()
			.translate(scanCenter.x, scanCenter.y, scanCenter.z)
			.rotate(MathUtil.getFATOrientation(this.getOrientation()))
			.translate(-scanCenter.x, -scanCenter.y, -scanCenter.z);
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
				if (!this.checkPoint(e.position(), scanCenterWorld, topVec)) {
					return false;
				}
				return true;
			},
			entities,
			128
		);

		for (final LivingEntity entity : entities) {
			results.add(this.scanResultFromEntity(entity, scanCenterWorld, transformInv));
		}

		final double mergeDistSqr = 2 * 2;
		final List<PointWithCounter> points = new ArrayList<>();
		for (final Ship s0 : VSGameUtilsKt.getShipsIntersecting(level, scanBoxWorld)) {
			final LoadedServerShip s = world.getLoadedShips().getById(s0.getId());
			if (s == null) {
				continue;
			}
			if (
				!tmpBB.set(s.getWorldAABB())
					.transform(transformInv)
					.intersectsAABB(scanBox)
			) {
				continue;
			}
			for (final BlockPos b : ShipNetworkAttachment.get(s).getEnergyBlocks()) {
				if (!(level.getBlockEntity(b) instanceof EnergyBasedBlockEntity be)) {
					continue;
				}
				if (be.getHeat() < 12000) {
					continue;
				}
				final Vec3 p = VSGameUtilsKt.toWorldCoordinates(s, b.getCenter());
				if (this.checkPoint(p, scanCenterWorld, topVec)) {
					points.add(new PointWithCounter(p));
				}
			}
		}

		for (int i = points.size() - 1; i >= 0; i--) {
			final PointWithCounter p1 = points.get(i);
			boolean merged = false;
			for (int j = i - 1; j >= 0; j--) {
				final PointWithCounter p2 = points.get(j);
				if (p1.p.distanceToSqr(p2.p) > mergeDistSqr) {
					final PointWithCounter p3 = p1.add(p2);
					points.set(j, points.get(i - 1));
					points.set(i - 1, p3);
					merged = true;
					break;
				}
			}
			if (!merged) {
				results.add(this.scanResultFromPoint(p1, scanCenterWorld, transformInv));
			}
		}
	}

	@Override
	protected Object createPeripheral() {
		return new IRSensorPeripheral(this);
	}

	protected boolean checkPoint(final Vec3 pos, final Vec3 scanCenter, final Vector3dc topVec) {
		final Vector3d dir = new Vector3d(pos.x - scanCenter.x, pos.y - scanCenter.y, pos.z - scanCenter.z);
		if (Math.abs(topVec.angle(dir)) < Math.PI / 4) {
			// not in sight
			return false;
		}
		final BlockHitResult hitResult = this.getLevel().clip(
			new BlockSourceClipContext(scanCenter, pos, ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, this.getBlockPos())
		);
		if (hitResult.getType() != HitResult.Type.MISS && hitResult.getLocation().distanceToSqr(pos) > 2 * 2) {
			return false;
		}
		if (
			RayCastUtil.rayCastEntity(
				this.getLevel(),
				scanCenter,
				pos,
				(e) -> e instanceof SmokeEntity || (e instanceof LivingEntity le && le.canBeSeenByAnyone())
			) != null
		) {
			return false;
		}
		return true;
	}

	private ScanResult scanResultFromEntity(final LivingEntity entity, final Vec3 scanCenter, final Matrix4dc transform) {
		final Vec3 pos = entity.position();
		final Vector3d relPos = new Vector3d(pos.x - scanCenter.x, pos.y - scanCenter.y, pos.z - scanCenter.z);
		final double distance = relPos.length();

		transform.transformDirection(relPos.normalize());
		final double xRot = Math.asin(-relPos.y);
		final double yRot = Math.atan2(relPos.x, relPos.z);

		final double width = entity.getBbWidth();
		final double height = entity.getBbHeight();
		return this.createScanResultWithError(ScanTypeInfo.ENTITY, distance, xRot, yRot, width, height);
	}

	private ScanResult scanResultFromPoint(final PointWithCounter point, final Vec3 scanCenter, final Matrix4dc transform) {
		final Vector3d relPos = new Vector3d(point.p.x - scanCenter.x, point.p.y - scanCenter.y, point.p.z - scanCenter.z);
		final double distance = relPos.length();

		transform.transformDirection(relPos.normalize());
		final double xRot = Math.asin(-relPos.y);
		final double yRot = Math.atan2(relPos.x, relPos.z);

		final AABBd projBox = point.box.transform(transform);
		final double width = projBox.lengthX();
		final double height = projBox.lengthY();
		return this.createScanResultWithError(ScanTypeInfo.SHIP, distance, xRot, yRot, width, height);
	}

	private ScanResult createScanResultWithError(
		final ScanTypeInfo type,
		double distance,
		double xRot,
		double yRot,
		double width,
		double height
	) {
		final double optRange = this.getOptimalScanRadius(width * height);
		final double optDiff = distance - optRange;
		if (optDiff > 0) {
			distance = applyError(distance, ERROR);
			xRot += MathUtil.normalizeAngle(generateRandom(Double.hashCode(xRot), -ANGLE_ERROR, ANGLE_ERROR));
			yRot += MathUtil.normalizeAngle(generateRandom(Double.hashCode(yRot), -ANGLE_ERROR, ANGLE_ERROR));
			width = applyError(width, ERROR);
			height = applyError(height, ERROR);
		}
		return new ScanResult(distance, xRot, yRot).appendInfo(type).appendInfo(new ScanSizeInfo(width, height));
	}

	private record PointWithCounter(Vec3 p, int c, AABBd box) {
		PointWithCounter(final Vec3 p) {
			this(p, 1, new AABBd(p.x - 0.5, p.y - 0.5, p.z - 0.5, p.x + 0.5, p.y + 0.5, p.z + 0.5));
		}

		public PointWithCounter add(final PointWithCounter other) {
			final int total = this.c + other.c;
			return new PointWithCounter(this.p.lerp(other.p, other.c / (double) (total)), total, this.box.union(other.box));
		}
	}
}
