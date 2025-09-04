package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.util.ShipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public abstract class JointBasedBlockEntity extends EnergyBasedBlockEntity implements IJointBlockEntity {
	protected JointBasedBlockEntity(final BlockEntityType<? extends JointBasedBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	@Override
	public ServerShip getPeerShip() {
		final BlockPos attachingBlock = this.getAttachingBlock();
		if (attachingBlock == null) {
			return null;
		}
		return ShipUtil.getServerShip((ServerLevel) (this.getLevel()), attachingBlock);
	}

	protected abstract int[] getConstraints();

	protected abstract void rebuildConstraints();

	protected void removeConstriants() {
		final int[] constraints = this.getConstraints();
		if (constraints == null) {
			return;
		}
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld((ServerLevel) (this.getLevel()));
		for (final int id : constraints) {
			world.removeConstraint(id);
		}
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		if (!level.isClientSide) {
			this.rebuildConstraints();
		}
	}

	@Override
	public void beforeRemove() {
		super.beforeRemove();
		if (!this.getLevel().isClientSide) {
			this.removeConstriants();
		}
	}
}
