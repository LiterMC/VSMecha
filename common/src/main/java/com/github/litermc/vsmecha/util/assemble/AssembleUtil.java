package com.github.litermc.vsmecha.util.assemble;

import com.github.litermc.vsmecha.compat.CompatMods;
import com.github.litermc.vsmecha.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaterniond;
import org.joml.RoundingMode;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class AssembleUtil {
	private AssembleUtil() {}

	public static ServerShip createShip(final ServerLevel level, final String slug, final Set<BlockPos> blocks, final ServerShip rootShip) {
		final ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
		final String levelId = VSGameUtilsKt.getDimensionId(level);

		final AABBd blocksBox = new AABBd();
		{
			final BlockPos pos = blocks.iterator().next();
			blocksBox.setMin(pos.getX(), pos.getY(), pos.getZ()).setMax(pos.getX(), pos.getY(), pos.getZ());
		}
		for (final BlockPos pos : blocks) {
			blocksBox.union(pos.getX(), pos.getY(), pos.getZ());
		}
		blocksBox.maxX += 1;
		blocksBox.maxY += 1;
		blocksBox.maxZ += 1;

		final Vector3i worldCenter = new Vector3i(blocksBox.center(new Vector3d()), RoundingMode.TRUNCATE);
		final ServerShip ship = shipWorld.createNewShipAtBlock(worldCenter, false, 1.0, levelId);
		final Vector3i shipCenter = ship.getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i());
		final Vector3i offset = shipCenter.sub(worldCenter, new Vector3i());
		final List<Pair<BlockPos, BlockState>> blockStates = new ArrayList<>(blocks.size());
		final List<Entity> entities = new ArrayList<>();

		ship.setSlug(slug);

		// get attachable entities
		for (final Entity entity : level.getEntities(null, new AABB(blocksBox.minX - 1, blocksBox.minY - 1, blocksBox.minZ - 1, blocksBox.maxX + 2, blocksBox.maxY + 2, blocksBox.maxZ + 2))) {
			if (entity instanceof final HangingEntity he) {
				final BlockPos hanging = he.getPos().relative(he.getDirection().getOpposite());
				if (blocks.contains(hanging)) {
					entities.add(entity);
				}
			} else if (CompatMods.CREATE.isLoaded()) {
				if (entity instanceof final SeatEntity seat) {
					if (blocks.contains(seat.blockPosition())) {
						entities.add(entity);
					}
				} else if (entity instanceof final SuperGlueEntity glue) {
					final AABB bb = glue.getBoundingBox();
					if (streamBlocksInAABB(bb).anyMatch(blocks::contains)) {
						entities.add(entity);
					}
				}
			}
		}

		// move blocks
		for (final BlockPos pos : blocks) {
			final BlockPos target = pos.offset(offset.x, offset.y, offset.z);
			BlockState state = level.getBlockState(pos);

			final BlockEntity be = level.getBlockEntity(pos);
			IMoveable<?> moveableOld = MoveUtil.getMover(be);
			if (moveableOld == null) {
				moveableOld = MoveUtil.getMover(state.getBlock());
			}
			if (moveableOld != null) {
				moveableOld.beforeSaveForMove(level, pos, target);
			}

			state = level.getBlockState(pos);
			blockStates.add(new Pair<>(pos, state));
			final CompoundTag nbt = level.getChunkAt(pos).getBlockEntityNbtForSaving(pos);
			if (nbt != null) {
				final BlockPos targetPos = pos.offset(offset.x, offset.y, offset.z);
				nbt.putInt("x", targetPos.getX());
				nbt.putInt("y", targetPos.getY());
				nbt.putInt("z", targetPos.getZ());
				level.getChunkAt(targetPos).setBlockEntityNbt(nbt);
			}

			Clearable.tryClear(be);

			final Object moveData = moveableOld != null ? moveableOld.beforeMove(level, pos, target) : null;
			level.setBlock(target, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_MOVE_BY_PISTON);
			// Note: Block.UPDATE_SUPPRESS_DROPS only works for Level.destroyBlock which drop the block's item form,
			// and it does not prevent contents from dropping.
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_MOVE_BY_PISTON);
			IMoveable<?> moveableNew = MoveUtil.getMover(level.getBlockEntity(target));
			if (moveableNew == null) {
				moveableNew = MoveUtil.getMover(state.getBlock());
			}
			if (moveableNew != null) {
				((IMoveable) (moveableNew)).afterMove(level, pos, target, moveData);
			}
		}

		// move entities
		for (final Entity entity : entities) {
			final Vec3 pos = entity.position();
			entity.setPos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
		}

		// update blocks
		for (final Pair<BlockPos, BlockState> value : blockStates) {
			final BlockPos pos = value.left();
			final Block block = value.right().getBlock();
			level.blockUpdated(pos, block);
			level.blockUpdated(pos.offset(offset.x, offset.y, offset.z), block);
		}

		final AABBic box = ship.getShipAABB();
		final Vector3d absPosition = ship.getTransform().getPositionInWorld().add(ship.getInertiaData().getCenterOfMassInShip(), new Vector3d()).sub(shipCenter.x, shipCenter.y, shipCenter.z);
		final Vector3d position = new Vector3d(absPosition);
		final Quaterniond rotation = new Quaterniond();
		final Vector3d velocity = new Vector3d();
		final Vector3d omega = new Vector3d();
		final Vector3d scaling = new Vector3d(1);
		double scale = 1.0;

		if (rootShip != null) {
			final ShipTransform selfTransform = rootShip.getTransform();
			selfTransform.getShipToWorld().transformPosition(position);
			rotation.set(selfTransform.getShipToWorldRotation());
			velocity.set(rootShip.getVelocity());
			omega.set(rootShip.getOmega());
			scaling.set(selfTransform.getShipToWorldScaling());
			scale = Math.sqrt(scaling.lengthSquared() / 3);
		}
		shipWorld.teleportShip(ship, new ShipTeleportDataImpl(position, rotation, velocity, omega, levelId, scale));

		// fix new ship's velocity and omega
		if (velocity.lengthSquared() != 0 || omega.lengthSquared() != 0) {
			ship.setTransformProvider(new ServerShipTransformProvider() {
				@Override
				public NextTransformAndVelocityData provideNextTransformAndVelocity(final ShipTransform transform, final ShipTransform nextTransform) {
					if (!transform.getPositionInWorld().equals(nextTransform.getPositionInWorld()) || !transform.getShipToWorldRotation().equals(nextTransform.getShipToWorldRotation())) {
						ship.setTransformProvider(null);
						return null;
					}
					if (ship.getVelocity().lengthSquared() == 0 && ship.getOmega().lengthSquared() == 0) {
						if (rootShip != null) {
							final ShipTransform selfTransform2 = rootShip.getTransform();
							selfTransform2.getShipToWorld().transformPosition(absPosition, position);
							rotation.set(selfTransform2.getShipToWorldRotation());
							velocity.set(rootShip.getVelocity());
							omega.set(rootShip.getOmega());
							scaling.set(selfTransform2.getShipToWorldScaling());
						}
						return new NextTransformAndVelocityData(new ShipTransformImpl(position, nextTransform.getPositionInShip(), rotation, scaling), velocity, omega);
					}
					return null;
				}
			});
		}
		return ship;
	}

	private static Stream<BlockPos> streamBlocksInAABB(AABB box) {
		final int
			minX = (int) (Math.round(box.minX)), maxX = (int) (Math.round(box.maxX)),
			minY = (int) (Math.round(box.minY)), maxY = (int) (Math.round(box.maxY)),
			minZ = (int) (Math.round(box.minZ)), maxZ = (int) (Math.round(box.maxZ));
		final int widthX = maxX - minX, widthY = maxY - minY, widthZ = maxZ - minZ;
		return IntStream.range(0, widthX * widthY * widthZ).mapToObj((i) -> {
			final int x = i % widthX + minX;
			i /= widthX;
			final int z = i % widthZ + minZ;
			i /= widthZ;
			final int y = i + minY;
			return new BlockPos(x, y, z);
		});
	}
}
