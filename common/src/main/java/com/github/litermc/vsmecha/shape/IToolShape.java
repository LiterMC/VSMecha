package com.github.litermc.vsmecha.shape;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

import org.joml.Vector3dc;

public interface IToolShape {
	boolean isCorrectToolForDrops(BlockState state);

	boolean test(ServerLevel level, Vector3dc pos, Vector3dc reaction);
}
