package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.VSMechaRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GlimbleServoHeadBlockEntity extends AbstractServoHeadBlockEntity {
	public GlimbleServoHeadBlockEntity(final BlockEntityType<? extends GlimbleServoHeadBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public GlimbleServoHeadBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.GLIMBLE_SERVO_HEAD.get(), pos, state);
	}
}
