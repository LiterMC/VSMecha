package com.github.litermc.vsmecha.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public final class ShipUtil {
	private static final Quaterniondc ZERO_QUAT = new Quaterniond();

	private ShipUtil() {}

	public static BlockPos toWorldBlockPos(final Level level, final BlockPos pos) {
		if (!VSGameUtilsKt.isBlockInShipyard(level, pos)) {
			return pos;
		}
		final Vector3d wpos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		VSGameUtilsKt.toWorldCoordinates(level, wpos);
		return BlockPos.containing(wpos.x, wpos.y, wpos.z);
	}

	public static ServerShip getServerShip(final ServerLevel level, final BlockPos pos) {
		final VsiServerShipWorld world = VSGameUtilsKt.getShipObjectWorld(level);
		final ServerShip ship = world.getAllShips().getByChunkPos(
			SectionPos.blockToSectionCoord(pos.getX()),
			SectionPos.blockToSectionCoord(pos.getZ()),
			VSGameUtilsKt.getDimensionId(level)
		);
		if (ship == null) {
			return null;
		}
		final LoadedServerShip loaded = world.getLoadedShips().getById(ship.getId());
		return loaded == null ? ship : loaded;
	}

	public static long getShipOrDimId(final ServerLevel level, final BlockPos pos) {
		final VsiServerShipWorld world = VSGameUtilsKt.getShipObjectWorld(level);
		final ServerShip ship = world.getAllShips().getByChunkPos(
			SectionPos.blockToSectionCoord(pos.getX()),
			SectionPos.blockToSectionCoord(pos.getZ()),
			VSGameUtilsKt.getDimensionId(level)
		);
		if (ship != null) {
			return ship.getId();
		}
		return world.getDimensionToGroundBodyIdImmutable().get(VSGameUtilsKt.getDimensionId(level));
	}

	public static long getShipOrDimId(final ServerLevel level, final ServerShip ship) {
		if (ship != null) {
			return ship.getId();
		}
		return VSGameUtilsKt.getShipObjectWorld(level).getDimensionToGroundBodyIdImmutable().get(VSGameUtilsKt.getDimensionId(level));
	}

	/**
	 * @return rotation {@code other} relative to {@code ship}
	 */
	public static Quaterniond getShipRelativeRotation(final ServerShip ship, final ServerShip other) {
		final Quaterniond baseRot = ship == null ? new Quaterniond() : new Quaterniond(ship.getTransform().getShipToWorldRotation());
		final Quaterniondc otherRot = other == null ? ZERO_QUAT : other.getTransform().getShipToWorldRotation();
		return baseRot.invert().mul(otherRot);
	}

	/**
	 * @return rotation {@code other} relative to {@code ship}
	 */
	public static Quaterniond getShipRelativeRotation(final PhysShip ship, final PhysShip other) {
		final Quaterniond baseRot = ship == null ? new Quaterniond() : new Quaterniond(ship.getTransform().getShipToWorldRotation());
		final Quaterniondc otherRot = other == null ? ZERO_QUAT : other.getTransform().getShipToWorldRotation();
		return baseRot.invert().mul(otherRot);
	}
}
