package com.github.litermc.vsmecha.entity;

import com.github.litermc.vsmecha.block.control.CapsuleSeatBlockEntity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

public class SeatEntity extends Entity {
	private BlockPos pos;

	public SeatEntity(final EntityType<? extends SeatEntity> type, final Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setInvisible(true);
		this.setInvulnerable(true);
	}

	public BlockPos getAttachedBlockPos() {
		return this.pos;
	}

	public void setAttachedBlockPos(final BlockPos pos) {
		this.pos = pos;
	}

	public CapsuleSeatBlockEntity getAttachedBlock() {
		if (this.pos == null) {
			return null;
		}
		return this.level().getBlockEntity(this.pos) instanceof CapsuleSeatBlockEntity be ? be : null;
	}

	public boolean wasDismount() {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public boolean shouldRender(final double x, final double y, final double z) {
		return false;
	}

	@Override
	public void tick() {
		if (!(this.level() instanceof ServerLevel)) {
			return;
		}
		final CapsuleSeatBlockEntity block = this.getAttachedBlock();
		if (block == null || block.getSeatEntity() != this) {
			this.discard();
		}
	}

	@Override
	protected void defineSynchedData() {}

	@Override
	protected void readAdditionalSaveData(final CompoundTag data) {
		final int[] pos = data.getIntArray("AttachedPos");
		this.pos = new BlockPos(pos[0], pos[1], pos[2]);
	}

	@Override
	protected void addAdditionalSaveData(final CompoundTag data) {
		data.putIntArray("AttachedPos", new int[]{this.pos.getX(), this.pos.getY(), this.pos.getZ()});
	}

	@Override
	public LivingEntity getControllingPassenger() {
		return null;
	}

	@Override
	public boolean canChangeDimensions() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE;
	}

	@Override
	public void setDeltaMovement(final Vec3 movement) {}

	public static class Renderer extends EntityRenderer<SeatEntity> {
		public Renderer(final EntityRendererProvider.Context ctx) {
			super(ctx);
		}

		@Override
		public boolean shouldRender(final SeatEntity entity, final Frustum frustum, final double x, final double y, final double z) {
			return false;
		}

		@Override
		public ResourceLocation getTextureLocation(final SeatEntity entity) {
			return null;
		}
	}
}
