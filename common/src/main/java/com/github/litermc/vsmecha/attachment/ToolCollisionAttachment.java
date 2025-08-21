package com.github.litermc.vsmecha.attachment;

import com.github.litermc.vsmecha.block.ToolBaseBlock;
import com.github.litermc.vsmecha.platform.PlatformHelper;
import com.github.litermc.vsmecha.util.DestroyUtil;
import com.github.litermc.vsmecha.util.IFakePlayer;
import com.github.litermc.vsmecha.util.PredictUtil;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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
import java.util.function.Predicate;
import java.util.stream.LongStream;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class ToolCollisionAttachment {
	private static final double COLLISION_EXTEND = 4.0 / 16;
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
		final Vector3dc scaling = ship.getTransform().getShipToWorldScaling();
		final double mass = ship.getInertiaData().getMass() * scaling.x() * scaling.y() * scaling.z() * 1e-3;

		final GameProfile profile = IFakePlayer.DEFAULT_PROFILE; // TODO: add a way to idenfity the owner
		final ServerPlayer player = PlatformHelper.get().createFakePlayer(level, profile);
		final IFakePlayer fakePlayer = ((IFakePlayer) (player));
		final Predicate<Entity> entityFilter = (e) -> !e.skipAttackInteraction(player);

		final Map<BlockPos, PredictUtil.BlockImpactData> impactedBlocks = new HashMap<>();
		final Map<Entity, PredictUtil.EntityImpactData> impactedEntities = new HashMap<>();

		final List<Ship> impactingShips = new ArrayList<>();
		impactingShips.add(null);
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

			for (final Ship other : VSGameUtilsKt.getShipsIntersecting(level, worldDetectBox)) {
				if (other.getId() == ship.getId()) {
					continue;
				}
				impactingShips.add(other);
			}
			PredictUtil.predict(ship).getImpacting(level, ship, pos, impactingShips, entityFilter, impactedBlocks, impactedEntities);
		}
		final double perMass = mass / (impactedBlocks.size() + impactedEntities.size());
		impactedBlocks.forEach((block, data) -> {
			fakePlayer.setDestroySpeed((float) (data.velocity * perMass));
			fakePlayer.setHasCorrectToolForDrops(data.hasCorrectToolForDrops ? data.state : null);
			DestroyUtil.impact(level, block, player);
		});
		impactedEntities.forEach((entity, data) -> {
			entity.hurt(player.damageSources().playerAttack(player), (float) (data.velocity * perMass) * data.damageAmplifier * 0.5f);
		});
	}
}
