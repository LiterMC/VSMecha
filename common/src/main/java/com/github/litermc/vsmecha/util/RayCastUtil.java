package com.github.litermc.vsmecha.util;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class RayCastUtil {
	private static final double MAX_ENTITY_BOX_SIZE = 27.7;

	private RayCastUtil() {}

	public static EntityHitResult rayCastEntity(final Level level, final Vec3 from, final Vec3 to, final Predicate<Entity> filter) {
		for (final AABB box : splitLine(from, to)) {
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

	public static void clipEntities(
		final Level level,
		final Vec3 from,
		final Vec3 to,
		final Predicate<Entity> filter,
		final BiConsumer<Entity, Vec3> consumer
	) {
		final IntOpenHashSet idSet = new IntOpenHashSet();
		final Predicate<Entity> filter0 = (entity) -> idSet.add(entity.getId()) && filter.test(entity);
		for (final AABB box : splitLine(from, to)) {
			for (final Entity entity : level.getEntities((Entity) null, box, filter0)) {
				final Vec3 pos = entity.getBoundingBox().clip(from, to).orElse(null);
				if (pos == null) {
					continue;
				}
				consumer.accept(entity, pos);
			}
		}
	}

	private static List<AABB> splitLine(final Vec3 from, final Vec3 to) {
		final int sections = (int) (from.distanceTo(to) / MAX_ENTITY_BOX_SIZE);
		if (sections == 0) {
			return Collections.singletonList(new AABB(from, to));
		}
		final List<AABB> boxes = new ArrayList<>();
		final Vec3 unit = from.vectorTo(to).normalize().scale(MAX_ENTITY_BOX_SIZE);
		Vec3 begin, end = from;
		for (int i = 0; i < sections; i++) {
			begin = end;
			end = begin.add(unit);
			boxes.add(new AABB(begin, end));
		}
		if (!end.equals(to)) {
			boxes.add(new AABB(end, to));
		}
		return boxes;
	}
}
