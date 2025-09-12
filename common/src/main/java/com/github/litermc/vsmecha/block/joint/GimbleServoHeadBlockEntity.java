package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.VSMechaRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GimbleServoHeadBlockEntity extends AbstractServoHeadBlockEntity {
	public GimbleServoHeadBlockEntity(final BlockEntityType<? extends GimbleServoHeadBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public GimbleServoHeadBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.GIMBLE_SERVO_HEAD.get(), pos, state);
	}
}
