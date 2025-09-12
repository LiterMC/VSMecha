package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.VSMechaRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GimbalServoHeadBlockEntity extends AbstractServoHeadBlockEntity {
	public GimbalServoHeadBlockEntity(final BlockEntityType<? extends GimbalServoHeadBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public GimbalServoHeadBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.GIMBAL_SERVO_HEAD.get(), pos, state);
	}
}
