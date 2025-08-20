package com.github.litermc.vsmecha.attachment;

import com.github.litermc.vsmecha.block.ToolBaseBlock;
import com.github.litermc.vsmecha.util.DestroyUtil;
import com.github.litermc.vsmecha.util.PredictUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ChunkClaim;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class ToolCollisionAttachment {
	private static final double COLLISION_EXTEND = 2.0 / 16;
	private final Set<BlockPos> toolBlocks = new HashSet<>();

	public ToolCollisionAttachment() {
	}

	public static ToolCollisionAttachment get(final ServerShip ship) {
		ToolCollisionAttachment attachment = ship.getAttachment(ToolCollisionAttachment.class);
		if (attachment == null) {
			attachment = new ToolCollisionAttachment();
			ship.saveAttachment(ToolCollisionAttachment.class, attachment);
		}
		return attachment;
	}

	@JsonGetter("toolBlocks")
	private long[] getToolBlocks() {
		return this.toolBlocks.stream().mapToLong(BlockPos::asLong).toArray();
	}

	@JsonSetter("toolBlocks")
	private void setToolBlocks(final long[] positions) {
		this.toolBlocks.clear();
		LongStream.of(positions).mapToObj(BlockPos::of).forEach(this.toolBlocks::add);
	}

	public void addBlock(final BlockPos pos) {
		this.toolBlocks.add(pos);
	}

	public void tick(final ServerLevel level, final LoadedServerShip ship) {
		final ChunkClaim claim = ship.getChunkClaim();
		final Matrix4dc mat = ship.getTransform().getShipToWorld();
		final Matrix4dc matR = ship.getTransform().getWorldToShip();
		final Vector3dc scaling = ship.getTransform().getShipToWorldScaling();
		final double mass = ship.getInertiaData().getMass() * scaling.x() * scaling.y() * scaling.z() / 50;

		final Map<BlockPos, Double> impactedBlocks = new HashMap<>();

		final Matrix4d tmpMat = new Matrix4d();
		final AABBd tmpAABB = new AABBd();
		final List<BlockPos> tmpPosList = new ArrayList<>();
		final List<Ship> impactingShips = new ArrayList<>();
		final Iterator<BlockPos> iter = this.toolBlocks.iterator();
		while (iter.hasNext()) {
			final BlockPos pos = iter.next();
			if (
				!claim.contains(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())) ||
				!(level.getBlockState(pos).getBlock() instanceof ToolBaseBlock)
			) {
				iter.remove();
				continue;
			}
			final AABBd detectBox = new AABBd(
				pos.getX() - COLLISION_EXTEND, pos.getY() - COLLISION_EXTEND, pos.getZ() - COLLISION_EXTEND,
				pos.getX() + 1 + COLLISION_EXTEND, pos.getY() + 1 + COLLISION_EXTEND, pos.getZ() + 1 + COLLISION_EXTEND
			);
			final AABBd worldDetectBox = detectBox.transform(mat, new AABBd());
			tmpPosList.clear();
			BlockPos.betweenClosedStream(
				Mth.floor(worldDetectBox.minX), Mth.floor(worldDetectBox.minY), Mth.floor(worldDetectBox.minZ),
				Mth.floor(worldDetectBox.maxX), Mth.floor(worldDetectBox.maxY), Mth.floor(worldDetectBox.maxZ)
			)
				.map(BlockPos::immutable)
				.forEach(tmpPosList::add);
			for (final BlockPos p : tmpPosList) {
				final VoxelShape shape = level.getBlockState(p).getCollisionShape(level, p);
				if (shape.isEmpty()) {
					continue;
				}
				final AABB bounds = shape.bounds().move(p);
					tmpAABB
						.setMin(bounds.minX, bounds.minY, bounds.minZ)
						.setMax(bounds.maxX, bounds.maxY, bounds.maxZ)
						.transform(matR);
				if (tmpAABB
						.intersectsAABB(detectBox)
				) {
					impactingShips.add(null);
				}
			}

			for (final Ship other : VSGameUtilsKt.getShipsIntersecting(level, worldDetectBox)) {
				if (other.getId() == ship.getId()) {
					continue;
				}
				matR.mul(other.getTransform().getShipToWorld(), tmpMat);
				worldDetectBox.transform(other.getTransform().getWorldToShip(), tmpAABB);
				tmpPosList.clear();
				BlockPos.betweenClosedStream(
					Mth.floor(tmpAABB.minX), Mth.floor(tmpAABB.minY), Mth.floor(tmpAABB.minZ),
					Mth.floor(tmpAABB.maxX), Mth.floor(tmpAABB.maxY), Mth.floor(tmpAABB.maxZ)
				)
					.map(BlockPos::immutable)
					.forEach(tmpPosList::add);
				for (final BlockPos p : tmpPosList) {
					final VoxelShape shape = level.getBlockState(p).getCollisionShape(level, p);
					if (shape.isEmpty()) {
						continue;
					}
					final AABB bounds = shape.bounds().move(p);
					if (
						tmpAABB
							.setMin(bounds.minX, bounds.minY, bounds.minZ)
							.setMax(bounds.maxX, bounds.maxY, bounds.maxZ)
							.transform(tmpMat)
							.intersectsAABB(detectBox)
					) {
						impactingShips.add(other);
					}
				}
			}
			PredictUtil.predict(ship).getImpacting(pos, impactingShips, impactedBlocks);
		}
		final double perMass = mass / impactedBlocks.size();
		impactedBlocks.forEach((block, vel) -> {
			DestroyUtil.impact(level, block, null, (float) (vel * perMass));
		});
	}
}
