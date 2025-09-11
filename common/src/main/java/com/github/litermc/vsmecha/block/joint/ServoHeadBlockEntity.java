package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.VSMechaRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ServoHeadBlockEntity extends AbstractServoHeadBlockEntity {
	public ServoHeadBlockEntity(final BlockEntityType<? extends ServoHeadBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public ServoHeadBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.SERVO_HEAD.get(), pos, state);
	}
}
