package com.github.litermc.vsmecha.entity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SmokeEntity extends Entity {
	private int remainingTicks = 0;

	public SmokeEntity(final EntityType<? extends SmokeEntity> type, final Level level) {
		super(type, level);
	}

	public void setRemainingTicks(final int remainingTicks) {
		this.remainingTicks = remainingTicks;
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
	protected void defineSynchedData() {}

	@Override
	protected void readAdditionalSaveData(final CompoundTag data) {
		this.remainingTicks = data.getInt("RemainingTicks");
	}

	@Override
	protected void addAdditionalSaveData(final CompoundTag data) {
		data.putInt("AttachedPos", this.remainingTicks);
	}

	@Override
	public void tick() {
		this.remainingTicks--;
		if (this.remainingTicks <= 0) {
			this.discard();
			return;
		}
		final AABB bb = this.getBoundingBox();
		// TODO: spawn smoke particles
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

	public static class Renderer extends EntityRenderer<SmokeEntity> {
		public Renderer(final EntityRendererProvider.Context ctx) {
			super(ctx);
		}

		@Override
		public boolean shouldRender(final SmokeEntity entity, final Frustum frustum, final double x, final double y, final double z) {
			return false;
		}

		@Override
		public ResourceLocation getTextureLocation(final SmokeEntity entity) {
			return null;
		}
	}
}
