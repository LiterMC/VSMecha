package com.github.litermc.vsmecha.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

public final class RayCastUtil {
	private static final double MAX_ENTITY_BOX_SIZE = 27.7;

	private RayCastUtil() {}

	public static EntityHitResult rayCastEntity(final Level level, final Vec3 from, final Vec3 to, final Predicate<Entity> filter) {
		final Vec3 unit = from.vectorTo(to).normalize().scale(MAX_ENTITY_BOX_SIZE);
		final int sections = (int) (unit.length() / MAX_ENTITY_BOX_SIZE);
		Vec3 begin, end = from;
		for (int i = 0; i < sections; i++) {
			begin = end;
			end = begin.add(unit);
			final AABB box = new AABB(begin, end);
			final List<Entity> entities = level.getEntities((Entity) null, box, filter);
			final EntityHitResult hit = clipNearestEntity(entities, from, to);
			if (hit != null) {
				return hit;
			}
		}
		if (!end.equals(to)) {
			final AABB box = new AABB(end, to);
			final List<Entity> entities = level.getEntities((Entity) null, box, filter);
			final EntityHitResult hit = clipNearestEntity(entities, from, to);
			if (hit != null) {
				return hit;
			}
		}
		return null;
	}

	private static EntityHitResult clipNearestEntity(final List<Entity> entities, final Vec3 from, final Vec3 to) {
		Entity nearestEntity = null;
		Vec3 hitPos = null;
		double nearestDist = 0;

		for (final Entity entity : entities) {
			final Vec3 pos = entity.getBoundingBox().clip(from, to).orElse(null);
			if (pos == null) {
				continue;
			}
			final double distance = from.distanceToSqr(pos);
			if (nearestEntity == null || distance < nearestDist) {
				nearestEntity = entity;
				hitPos = pos;
				nearestDist = distance;
			}
		}
		return nearestEntity == null ? null : new EntityHitResult(nearestEntity, hitPos);
	}
}
