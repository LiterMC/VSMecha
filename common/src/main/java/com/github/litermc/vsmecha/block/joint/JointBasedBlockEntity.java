package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.Constants;
import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.util.ShipUtil;
import com.github.litermc.vtil.util.TaskUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.core.internal.joints.VSJoint;
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

	protected abstract void tryRebuildJoints();

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

	protected boolean updateJoints(final PhysLevel world, final VSJoint... joints) {
		final VsiPhysLevel physWorld = (VsiPhysLevel) world;
		final int[] jointIds = this.getJointIds();
		if (jointIds == null) {
			Constants.LOG.warn("Trying to update joints which were already removed. {} ({})", this.getLevel().dimension(), this.getBlockPos());
			return false;
		}
		if (jointIds.length != joints.length) {
			Constants.LOG.error("New joints length ({}) must match existing joints length ({}). {} ({})", joints.length, jointIds.length, this.getLevel().dimension(), this.getBlockPos());
			throw new IllegalArgumentException("joints length mismatch");
		}
		boolean allSuccess = true;
		for (int i = 0; i < joints.length; i++) {
			final VSJoint joint = joints[i];
			if (joint == null) {
				continue;
			}
			final int jointId = jointIds[i];
			allSuccess = physWorld.updateJoint(jointId, joint) && allSuccess;
		}
		return allSuccess;
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		if (!level.isClientSide) {
			this.tryRebuildJoints();
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
