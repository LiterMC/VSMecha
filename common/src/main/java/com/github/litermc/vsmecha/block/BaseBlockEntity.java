package com.github.litermc.vsmecha.block;

import com.github.litermc.vsmecha.attachment.ShipNetworkAttachment;
import com.github.litermc.vsmecha.util.ShipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.valkyrienskies.core.api.ships.ServerShip;

public abstract class BaseBlockEntity extends BlockEntity {
	protected BaseBlockEntity(final BlockEntityType<? extends BaseBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		this.saveShared(data);
	}

	protected void saveShared(final CompoundTag data) {
	}

	@Override
	public CompoundTag getUpdateTag() {
		final CompoundTag data = super.getUpdateTag();
		this.saveShared(data);
		return data;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	public void sendUpdate() {
		final Level level = this.getLevel();
		if (level == null || level.isClientSide) {
			return;
		}
		this.setChanged();
		level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 0 /* no use on server-side */);
	}

	public void serverTick() {
		final ServerShip ship = ShipUtil.getServerShip((ServerLevel) (this.getLevel()), this.getBlockPos());
		if (ship != null) {
			ShipNetworkAttachment.get(ship).addBlockEntity(this);
		}
	}

	public void clientTick() {}
}
