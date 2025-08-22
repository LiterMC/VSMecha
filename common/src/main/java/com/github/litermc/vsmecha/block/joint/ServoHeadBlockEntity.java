package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.block.BaseBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class ServoHeadBlockEntity extends BaseBlockEntity {
	private final Direction direction;
	BlockPos basePos = null;
	ServoBlockEntity.ServoInfo servoInfo = null;

	public ServoHeadBlockEntity(final BlockEntityType<? extends ServoHeadBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.direction = state.getValue(BlockStateProperties.FACING);
	}

	public ServoHeadBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.SERVO_HEAD.get(), pos, state);
	}

	public Direction getDirection() {
		return this.direction;
	}

	@Override
	public void serverTick() {
		if (this.basePos == null) {
			return;
		}
		if (this.servoInfo.detached) {
			this.basePos = null;
			this.servoInfo = null;
			return;
		}

		final ServerLevel level = (ServerLevel) (this.getLevel());
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(level);
		if (level.getBlockEntity(this.basePos) instanceof ServoBlockEntity sbe && sbe.servoInfo == this.servoInfo) {
			return;
		}
		world.removeConstraint(this.servoInfo.attachConstraintId);
		world.removeConstraint(this.servoInfo.rotateConstraintId);
		this.servoInfo.detached = true;
		this.servoInfo = null;
		this.basePos = null;
	}
}
