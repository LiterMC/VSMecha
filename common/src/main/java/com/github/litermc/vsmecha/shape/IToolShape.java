package com.github.litermc.vsmecha.shape;

import com.github.litermc.vsmecha.block.tool.ToolBaseBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3dc;

public interface IToolShape {
	boolean isCorrectToolForDrops(BlockState state);

	double getDestroySpeed(BlockState state);

	double damageAmplifier(Entity entity);

	boolean test(ServerLevel level, Vector3dc pos, Vector3dc reactionDir);
}

final class ShapeUtil {
	private ShapeUtil() {}

	static boolean isAirBlock(final ServerLevel level, final Vector3dc pos) {
		final BlockState state = level.getBlockState(BlockPos.containing(pos.x(), pos.y(), pos.z()));
		return state.isAir() || state.getBlock() instanceof LiquidBlock;
	}

	static boolean isToolBlock(final ServerLevel level, final Vector3dc pos) {
		return isToolBlock(level, BlockPos.containing(pos.x(), pos.y(), pos.z()));
	}

	static boolean isToolBlock(final ServerLevel level, final BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof ToolBaseBlock;
	}

	static boolean checkFlat(final boolean[] flags, final int index) {
		final int half = flags.length / 2;
		final int n = half / 2 / 2 + 1;
		final int low1 = index - n;
		final int high1 = index + n;
		final int low2 = low1 + half;
		final int high2 = high1 + half;
		if (low1 < 0 || high2 < flags.length) {
			for (int i = high2; i < flags.length + low1; i++) {
				if (flags[i % flags.length]) {
					return false;
				}
			}
		} else {
			for (int i = high2 - flags.length; i < low1; i++) {
				if (flags[i]) {
					return false;
				}
			}
		}
		for (int i = high1; i < low2; i++) {
			if (flags[i % flags.length]) {
				return false;
			}
		}
		return true;
	}
}
