package com.github.litermc.vsmecha.block;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.attachment.ToolCollisionAttachment;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class ToolBaseBlockEntity extends BaseBlockEntity {
	public ToolBaseBlockEntity(final BlockEntityType<? extends ToolBaseBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public ToolBaseBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.TOOL_BASE.get(), pos, state);
	}

	@Override
	public void serverTick() {
		final BlockPos pos = this.getBlockPos();
		final ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) (this.getLevel()), pos);
		if (ship != null) {
			ToolCollisionAttachment.get(ship).addBlock(pos);
		}
	}
}
