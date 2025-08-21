package com.github.litermc.vsmecha.shape;

import com.github.litermc.vsmecha.util.VecUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public class PickAxeShape implements IToolShape {
	public static final PickAxeShape INSTANCE = new PickAxeShape();

	@Override
	public boolean isCorrectToolForDrops(final BlockState state) {
		return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
	}

	@Override
	public double getDestroySpeed(final BlockState state) {
		return this.isCorrectToolForDrops(state) ? 10 : 1;
	}

	@Override
	public double damageAmplifier(final Entity entity) {
		return 1.5;
	}

	@Override
	public boolean test(final ServerLevel level, final Vector3dc pos, final Vector3dc reactionDir) {
		final Vector3d testPos = new Vector3d();
		for (int i = 1; i <= 3; i++) {
			if (!ShapeUtil.isToolBlock(level, testPos.set(reactionDir).mul(i).add(pos))) {
				return false;
			}
		}
		final Vector3d[] plane = VecUtil.generatePlaneVectors(reactionDir, 2);
		int count = 0;
		for (int i = 0; i < plane.length; i++) {
			if (!ShapeUtil.isAirBlock(level, testPos.set(plane[i]).mul(3).add(pos))) {
				count++;
				if (count > 2) {
					return false;
				}
			}
		}
		count = 0;
		VecUtil.planeVectorsToAngled(reactionDir, 70 * Math.PI / 180, plane);
		for (int i = 0; i < plane.length; i++) {
			if (!ShapeUtil.isAirBlock(level, testPos.set(plane[i]).mul(3.5).add(pos))) {
				count++;
				if (count > 4) {
					return false;
				}
			}
		}
		return true;
	}
}
