package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.util.ShipUtil;
import com.github.litermc.vtil.util.TaskUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.internal.world.VsiPhysLevel;

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

	protected abstract int[] getJointIds();

	protected abstract void rebuildJoints();

	protected void removeJoints() {
		final int[] joints = this.getJointIds();
		if (joints == null) {
			return;
		}
		TaskUtil.queuePhysicsTick((ServerLevel) (this.getLevel()), (world) -> {
			final VsiPhysLevel physWorld = (VsiPhysLevel) world;
			for (final int id : joints) {
				physWorld.removeJoint(id);
			}
		});
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		if (!level.isClientSide) {
			this.rebuildJoints();
		}
	}

	@Override
	public void beforeRemove() {
		super.beforeRemove();
		if (!this.getLevel().isClientSide) {
			this.removeJoints();
		}
	}
}
