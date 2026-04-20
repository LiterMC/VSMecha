package com.github.litermc.vsmecha.block.tool;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.attachment.ToolCollisionAttachment;
import com.github.litermc.vsmecha.block.BaseBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

// TODO: mixin EntityShipCollisionUtils.adjustEntityMovementForShipCollisions to avoid entity drag on tool block

public class ToolBaseBlockEntity extends BaseBlockEntity {
	public ToolBaseBlockEntity(final BlockEntityType<? extends ToolBaseBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public ToolBaseBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.TOOL_BASE.get(), pos, state);
	}

	@Override
	public void serverTick() {
		super.serverTick();

		final BlockPos pos = this.getBlockPos();
		final LoadedServerShip ship = VSGameUtilsKt.getLoadedShipManagingPos((ServerLevel) (this.getLevel()), pos);
		if (ship != null) {
			ToolCollisionAttachment.get(ship).addBlock(pos);
		}
	}
}
