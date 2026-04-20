package com.github.litermc.vsmecha.block;

import com.github.litermc.vsmecha.VSMechaRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class DummyBlockEntity extends BlockEntity {
	public DummyBlockEntity(final Level level, final BlockPos pos) {
		super(VSMechaRegistry.BlockEntities.DUMMY.get(), pos, Blocks.AIR.defaultBlockState());
		this.level = level;
	}
}
