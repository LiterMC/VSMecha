package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.compat.CompatMods;
import com.github.litermc.vsmecha.compat.computercraft.ElectroGraspPeripheral;
import com.github.litermc.vsmecha.util.BlockSourceClipContext;
import com.github.litermc.vsmecha.util.ShipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class ElectroGraspBlockEntity extends JointBasedBlockEntity implements IAttachableBlockEntity {
	private static final double ATTACH_COMPLIANCE = 0;
	private static final double ATTACH_MAX_FORCE = Double.POSITIVE_INFINITY;
	private static final double EXTEND_LOCK_AREA = 8.0 / 16;

	private final Direction direction;
	private BlockPos attachingBlock = null;
	private Vector3dc attachingPos = null;
	private Vector3dc pendingAttachPos = null;
	private Integer attachConstraintId = null;
	private boolean redstoneAttach = false;

	public ElectroGraspBlockEntity(final BlockEntityType<? extends ElectroGraspBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.direction = state.getValue(BlockStateProperties.FACING);
	}

	public ElectroGraspBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.ELECTRO_GRASP.get(), pos, state);
	}

	public Direction getDirection() {
		return this.direction;
	}

	@Override
	public BlockPos getAttachingBlock() {
		return this.attachingBlock;
	}

	@Override
	public boolean canTransferEnergy() {
		return false;
	}

	public int getEnergyConsumption() {
		return 100;
	}

	@Override
	public int getMaxHeatCapacity() {
		return 60000;
	}

	@Override
	public int getDangerousHeatLimit() {
		return 58000;
	}

	@Override
	public int getMaxEnergyStorage() {
		return this.getEnergyConsumption() * 2;
	}

	@Override
	public int getEnergyInputLimit() {
		return this.getMaxEnergyStorage();
	}

	@Override
	public int getEnergyOutputLimit() {
		return 0;
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		if (data.contains("AttachingBlock") && data.contains("AttachingPos")) {
			final int[] attachingBlockArr = data.getIntArray("AttachingBlock");
			this.attachingBlock = new BlockPos(attachingBlockArr[0], attachingBlockArr[1], attachingBlockArr[2]);
			final ListTag attachingPosList = data.getList("AttachingPos", Tag.TAG_DOUBLE);
			this.pendingAttachPos = new Vector3d(attachingPosList.getDouble(0), attachingPosList.getDouble(1), attachingPosList.getDouble(2));
		}
		this.redstoneAttach = data.getBoolean("RedstoneAttach");
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		if (this.attachingBlock != null) {
			data.putIntArray("AttachingBlock", new int[]{this.attachingBlock.getX(), this.attachingBlock.getY(), this.attachingBlock.getZ()});
			final Vector3dc attachingPos = this.attachingPos == null ? this.pendingAttachPos : this.attachingPos;
			final ListTag attachingPosList = new ListTag();
			attachingPosList.add(DoubleTag.valueOf(attachingPos.x()));
			attachingPosList.add(DoubleTag.valueOf(attachingPos.y()));
			attachingPosList.add(DoubleTag.valueOf(attachingPos.z()));
			data.put("AttachingPos", attachingPosList);
		}
		data.putBoolean("RedstoneAttach", redstoneAttach);
	}

	@Override
	public boolean attachTo(final BlockPos otherPos) {
		return false;
	}

	@Override
	public boolean detach() {
		this.redstoneAttach = false;
		if (this.attachingBlock == null) {
			return false;
		}
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(level);
		if (this.attachConstraintId != null) {
			world.removeConstraint(this.attachConstraintId);
			this.attachConstraintId = null;
		}
		this.attachingBlock = null;
		this.attachingPos = null;
		this.pendingAttachPos = null;
		return true;
	}

	@Override
	public boolean tryAttach() {
		if (!this.isEnabled()) {
			return false;
		}
		if (this.isAttached()) {
			this.detach();
		}
		if (this.getEnergyStored() < this.getEnergyConsumption()) {
			return false;
		}
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos pos = this.getBlockPos();
		final ServerShip ship = ShipUtil.getServerShip(level, pos);
		final Direction dir = this.getDirection();
		final Vector3dc mountPos = new Vector3d(
			pos.getX() + 0.5 + dir.getStepX() * 6.0 / 16,
			pos.getY() + 0.5 + dir.getStepY() * 6.0 / 16,
			pos.getZ() + 0.5 + dir.getStepZ() * 6.0 / 16
		);
		final Vector3d to = mountPos.add(
			dir.getStepX() * EXTEND_LOCK_AREA,
			dir.getStepY() * EXTEND_LOCK_AREA,
			dir.getStepZ() * EXTEND_LOCK_AREA,
			new Vector3d()
		);
		final Vector3d from = new Vector3d(mountPos);
		if (ship != null) {
			ship.getTransform().getShipToWorld().transformPosition(from);
			ship.getTransform().getShipToWorld().transformPosition(to);
		}

		final BlockHitResult hitResult = level.clip(new BlockSourceClipContext(
			new Vec3(from.x, from.y, from.z),
			new Vec3(to.x, to.y, to.z),
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
			pos
		));
		if (hitResult.getType() == HitResult.Type.MISS) {
			return false;
		}
		final BlockPos targetPos = hitResult.getBlockPos();
		// TODO: add mountable block tag?
		final ServerShip target = ShipUtil.getServerShip(level, targetPos);
		if (target != null) {
			target.getTransform().getWorldToShip().transformPosition(from);
		}
		final long selfId = ShipUtil.getShipOrDimId(level, ship);
		final long targetId = ShipUtil.getShipOrDimId(level, target);
		if (selfId == targetId) {
			return false;
		}

		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(level);
		final VSAttachmentConstraint attachConstraint = new VSAttachmentConstraint(
			selfId,
			targetId,
			ATTACH_COMPLIANCE,
			mountPos,
			from,
			ATTACH_MAX_FORCE,
			0
		);
		this.attachConstraintId = world.createNewConstraint(attachConstraint);
		this.attachingBlock = targetPos;
		this.attachingPos = mountPos;
		this.pendingAttachPos = null;
		this.redstoneAttach = false;
		return true;
	}

	@Override
	protected Object createPeripheral() {
		return new ElectroGraspPeripheral(this);
	}

	@Override
	protected int[] getConstraints() {
		return this.attachConstraintId == null ? null : new int[]{this.attachConstraintId};
	}

	@Override
	protected void rebuildConstraints() {
		final ServerLevel level = (ServerLevel) (this.getLevel());

		final Vector3dc pendingAttachPos = this.pendingAttachPos;
		if (pendingAttachPos == null) {
			return;
		}
		this.pendingAttachPos = null;

		final ServerShip ship = ShipUtil.getServerShip(level, this.getBlockPos());
		final ServerShip target = ShipUtil.getServerShip(level, this.attachingBlock);
		final long selfId = ShipUtil.getShipOrDimId(level, ship);
		final long targetId = ShipUtil.getShipOrDimId(level, target);
		if (selfId == targetId) {
			this.attachingBlock = null;
			this.setChanged();
			return;
		}

		final Vector3d targetMountPos = new Vector3d(pendingAttachPos);
		if (ship != null) {
			ship.getTransform().getShipToWorld().transformPosition(targetMountPos);
		}
		if (target != null) {
			target.getTransform().getWorldToShip().transformPosition(targetMountPos);
		}
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(level);
		final VSAttachmentConstraint attachConstraint = new VSAttachmentConstraint(
			selfId,
			targetId,
			ATTACH_COMPLIANCE,
			pendingAttachPos,
			targetMountPos,
			ATTACH_MAX_FORCE,
			0
		);
		this.attachConstraintId = world.createNewConstraint(attachConstraint);
		this.attachingPos = pendingAttachPos;
	}

	@Override
	public void neighborChanged(final Block neighbor, final BlockPos neighborPos, final boolean moving) {
		super.neighborChanged(neighbor, neighborPos, moving);
		if (!(this.getLevel() instanceof ServerLevel level)) {
			return;
		}
		final boolean wantAttach = level.hasNeighborSignal(this.getBlockPos());
		if (this.redstoneAttach) {
			if (!wantAttach) {
				this.detach();
			}
		} else if (wantAttach && !this.isAttached() && this.tryAttach()) {
			this.redstoneAttach = true;
		}
	}

	@Override
	public void serverTick() {
		super.serverTick();

		final ServerLevel level = (ServerLevel) (this.getLevel());
		if (!this.isEnabled()) {
			this.detach();
			return;
		}

		if (this.isAttached()) {
			final int newEnergy = this.getEnergyStored() - this.getEnergyConsumption();
			if (newEnergy < 0) {
				this.detach();
				return;
			}
			this.setEnergyStored(newEnergy);
		}

		if (this.attachingBlock == null) {
			return;
		}
		final BlockState state = level.getBlockState(this.attachingBlock);
		if (state.isAir() || state.canBeReplaced()) {
			this.detach();
		}
	}
}
