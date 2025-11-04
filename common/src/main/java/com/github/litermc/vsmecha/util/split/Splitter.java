package com.github.litermc.vsmecha.util.split;

import com.github.litermc.vsmecha.util.LevelUtil;
import com.github.litermc.vsmecha.util.assemble.AssembleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class Splitter {
	private Splitter() {}

	private static final Map<LoadedServerShip, Map<BlockPos, OldStateHolder>> UPDATING_SHIP = new HashMap<>();

	/**
	 * module-private
	 */
	public static void onBlockUpdated(final ServerLevel level, final BlockPos pos, final BlockState oldState, final BlockState newState) {
		final LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(level, pos);
		if (ship == null) {
			return;
		}
		final Map<BlockPos, OldStateHolder> updates = UPDATING_SHIP.computeIfAbsent(ship, (s) -> new HashMap<>());
		if (updates.containsKey(pos)) {
			return;
		}
		if (isAir(oldState)) {
			updates.put(pos, new OldStateHolder(oldState, List.of()));
		} else {
			final List<BlockPos> prevConns = new ArrayList<>(6);
			getConnectableBlocks(level, pos, oldState, prevConns);
			updates.put(pos, new OldStateHolder(oldState, prevConns));
		}
	}

	public static void postServerTick() {
		for (final Map.Entry<LoadedServerShip, Map<BlockPos, OldStateHolder>> entry : UPDATING_SHIP.entrySet()) {
			final LoadedServerShip ship = entry.getKey();
			final Map<BlockPos, OldStateHolder> updates = entry.getValue();
			final ServerLevel level = LevelUtil.getLevel(ship.getChunkClaimDimension());
			final List<Set<BlockPos>> parts = getSeparatedParts(level, ship, updates);
			if (parts == null) {
				continue;
			}
			final String slug = ship.getSlug();
			for (final Set<BlockPos> part : parts) {
				AssembleUtil.createShip(level, null, part, ship);
			}
		}
		UPDATING_SHIP.clear();
	}

	/**
	 * Split a ship after block updates.
	 * @param level   The level the ship is in.
	 * @param ship    The ship.
	 * @param updates Updated block positions and its last connections.
	 * @return
	 *     {@code null} if the ship should not split.
	 *     Otherwise, one or more disjoint BlockPos sets will returns to indicate the parts to be split out.
	 */
	public static List<Set<BlockPos>> getSeparatedParts(final Level level, final Ship ship, final Map<BlockPos, OldStateHolder> updates) {
		final Map<BlockPos, PartHolder> visited = new HashMap<>(32);
		final Set<PartHolder> parts = new HashSet<>(updates.size() * 2);

		final Set<BlockPos> nextPosSet = new HashSet<>(6);
		for (final Map.Entry<BlockPos, OldStateHolder> update : updates.entrySet()) {
			final BlockPos startBlock = update.getKey();
			final OldStateHolder prevState = update.getValue();
			final BlockState newState = level.getBlockState(startBlock);
			if (!willConnectivityChange(level, startBlock, prevState.state(), newState)) {
				continue;
			}
			if (!isAir(newState)) {
				final PartHolder p = new PartHolder(new Part(startBlock));
				parts.add(p);
				visited.put(startBlock, p);
			}
			getConnectableBlocks(level, startBlock, nextPosSet);
			for (final BlockPos conn : prevState.connections()) {
				if (!nextPosSet.contains(conn) && !updates.containsKey(conn) && !visited.containsKey(conn) && !isAir(level, conn)) {
					final PartHolder p = new PartHolder(new Part(conn));
					parts.add(p);
					visited.put(conn, p);
				}
			}
			nextPosSet.clear();
		}
		if (parts.size() <= 1) {
			return null;
		}

		final List<BlockPos> nextPoses = new ArrayList<>(6);

		final List<Set<BlockPos>> scannedParts = new ArrayList<>();
		final Set<PartHolder> partsCopy = new HashSet<>(parts.size());
		int polled;
		do {
			polled = 0;
			partsCopy.clear();
			partsCopy.addAll(parts);
			for (final PartHolder holder : partsCopy) {
				if (!parts.contains(holder)) {
					continue;
				}
				final Part part = holder.getForward().part;
				if (part.complete) {
					// TODO: why will this happen?
					parts.remove(holder);
					continue;
				}
				final BlockPos pos = part.poll();
				if (pos == null) {
					part.complete = true;
					scannedParts.add(part.blocks);
					parts.remove(part);
					continue;
				}
				polled++;
				final BlockState state = level.getBlockState(pos);
				// assert !isAir(state);
				nextPoses.clear();
				getConnectableBlocks(level, pos, state, nextPoses);
				for (final BlockPos p : nextPoses) {
					if (part.blocks.contains(p)) {
						continue;
					}
					final BlockState s = level.getBlockState(p);
					if (isAir(s)) {
						continue;
					}
					if (!isBlockConnectable(level, p, s, pos, state)) {
						continue;
					}
					PartHolder otherHolder = visited.get(p);
					if (otherHolder != null) {
						otherHolder = otherHolder.getForward();
						final Part otherPart = otherHolder.part;
						if (otherPart != part) {
							part.merge(otherPart);
							otherPart.complete = true;
							otherHolder.forward = holder;
							parts.remove(otherHolder);
							if (scannedParts.isEmpty() && parts.size() <= 1) {
								return null;
							}
						}
						continue;
					}
					part.add(p);
					visited.put(p, holder);
				}
			}
		} while (polled > 1);

		if (parts.isEmpty()) {
			scannedParts.remove(scannedParts.size() - 1);
		}
		return scannedParts;
	}

	public static boolean willConnectivityChange(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState oldState,
		final BlockState newState
	) {
		if (oldState == newState) {
			return true;
		}
		final boolean wasAir = isAir(oldState);
		if (wasAir != isAir(newState)) {
			return true;
		}
		if (!wasAir) {
			return true;
		}
		final IBlockAnchor anchor = (IBlockAnchor) (newState.getBlock());
		return anchor.willConnectivityChange(level, pos, oldState, newState);
	}

	private static final boolean isAir(final LevelAccessor level, final BlockPos pos) {
		return isAir(level.getBlockState(pos));
	}

	private static final boolean isAir(final BlockState state) {
		if (state.isAir()) {
			return true;
		}
		if (state.getBlock() instanceof LiquidBlock) {
			final FluidState fluidState = state.getFluidState();
			if (fluidState.isEmpty() || !fluidState.isSource()) {
				return true;
			}
		}
		return false;
	}

	private static final void getConnectableBlocks(final Level level, final BlockPos pos, final Collection<BlockPos> result) {
		final BlockState state = level.getBlockState(pos);
		if (isAir(state)) {
			return;
		}
		getConnectableBlocks(level, pos, state, result);
	}

	private static final void getConnectableBlocks(
		final Level level,
		final BlockPos pos,
		final BlockState state,
		final Collection<BlockPos> result
	) {
		final IBlockAnchor anchor = (IBlockAnchor) (state.getBlock());
		anchor.getConnectableBlocks(level, pos, state, result);
	}

	private static final boolean isBlockConnectable(
		final Level level,
		final BlockPos pos,
		final BlockState state,
		final BlockPos targetPos,
		final BlockState targetState
	) {
		final IBlockAnchor anchor = (IBlockAnchor) (state.getBlock());
		return anchor.isBlockConnectable(level, pos, state, targetPos, targetState);
	}

	public record OldStateHolder(BlockState state, Collection<BlockPos> connections) {}

	private static final class PartHolder {
		final Part part;
		PartHolder forward = null;

		PartHolder(final Part part) {
			this.part = part;
		}

		PartHolder getForward() {
			if (this.forward != null) {
				return this.forward.getForward();
			}
			return this;
		}
	}

	private static final class Part {
		final Set<BlockPos> blocks = new HashSet<>();
		final Queue<BlockPos> pending = new ArrayDeque<>();
		boolean complete = false;

		Part(final BlockPos startBlock) {
			this.add(startBlock);
		}

		void merge(final Part other) {
			if (other.complete) {
				throw new RuntimeException("Unexpected early stopped scan. Connectivity inconsistent?");
			}
			this.blocks.addAll(other.blocks);
			while (true) {
				final BlockPos pos = other.poll();
				if (pos == null) {
					break;
				}
				this.pending.add(pos);
			}
		}

		BlockPos poll() {
			return this.pending.poll();
		}

		void add(final BlockPos pos) {
			if (this.blocks.add(pos)) {
				this.pending.add(pos);
			}
		}
	}
}
