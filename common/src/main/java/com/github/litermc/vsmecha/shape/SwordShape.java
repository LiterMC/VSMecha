package com.github.litermc.vsmecha.shape;

import com.github.litermc.vsmecha.util.MathUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.HashSet;
import java.util.Set;

public class SwordShape implements IToolShape {
	public static final SwordShape INSTANCE = new SwordShape();

	@Override
	public boolean isCorrectToolForDrops(final BlockState state) {
		return state.is(Blocks.COBWEB);
	}

	@Override
	public double getDestroySpeed(final BlockState state) {
		if (state.is(Blocks.COBWEB)) {
			return 10;
		}
		return state.is(BlockTags.SWORD_EFFICIENT) ? 2 : 1;
	}

	@Override
	public double damageAmplifier(final Entity entity) {
		return 8;
	}

	@Override
	public boolean test(final ServerLevel level, final Vector3dc pos, final Vector3dc reactionDir) {
		final Vector3d testPos = new Vector3d(reactionDir).mul(5).add(pos);
		if (!ShapeUtil.isAirBlock(level, testPos)) {
			return false;
		}
		final Vector3d[] plane = MathUtil.generatePlaneVectors(reactionDir, 2);
		final boolean[] flags = new boolean[plane.length];
		final Set<BlockPos> planeBlocks = new HashSet<>();
		for (int i = 0; i < plane.length; i++) {
			testPos.set(plane[i]).add(pos);
			final BlockPos bpos = BlockPos.containing(testPos.x, testPos.y, testPos.z);
			if (ShapeUtil.isToolBlock(level, bpos)) {
				flags[i] = true;
				planeBlocks.add(bpos);
			}
		}
		if (planeBlocks.size() < 2) {
			MathUtil.planeVectorsToAngled(reactionDir, 45 * Math.PI / 180, plane);
			for (int i = 0; i < plane.length; i++) {
				testPos.set(plane[i]).add(pos);
				final BlockPos bpos = BlockPos.containing(testPos.x, testPos.y, testPos.z);
				if (!flags[i] && ShapeUtil.isToolBlock(level, bpos)) {
					flags[i] = true;
					planeBlocks.add(bpos);
				}
			}
			if (planeBlocks.size() < 2) {
				return false;
			}
		}
		for (int i = 0; i < flags.length; i++) {
			if (flags[i] && !ShapeUtil.checkFlat(flags, i)) {
				return false;
			}
		}
		return true;
	}
}
