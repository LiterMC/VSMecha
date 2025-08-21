package com.github.litermc.vsmecha.shape;

import com.github.litermc.vsmecha.util.VecUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public class PickAxeShape implements IToolShape {
	public static final PickAxeShape INSTANCE = new PickAxeShape();

	@Override
	public boolean isCorrectToolForDrops(final BlockState state) {
		return true;
	}

	@Override
	public double damageAmplifier(final Entity entity) {
		return 1.5;
	}

	@Override
	public boolean test(final ServerLevel level, final Vector3dc pos, final Vector3dc reactionDir) {
		final Vector3d testPos = new Vector3d();
		for (int i = 1; i <= 4; i++) {
			if (!ShapeUtil.isToolBlock(level, testPos.set(reactionDir).mul(5).add(pos))) {
				return false;
			}
		}
		final Vector3d[] plane = VecUtil.generatePlaneVectors(reactionDir, 2);
		for (int i = 0; i < plane.length; i++) {
			if (!ShapeUtil.isAirBlock(level, testPos.set(plane[i]).mul(3).add(pos))) {
				return false;
			}
		}
		VecUtil.planeVectorsToAngled(reactionDir, 70 * Math.PI / 180, plane);
		for (int i = 0; i < plane.length; i++) {
			if (!ShapeUtil.isAirBlock(level, testPos.set(plane[i]).mul(4).add(pos))) {
				return false;
			}
		}
		return true;
	}
}
