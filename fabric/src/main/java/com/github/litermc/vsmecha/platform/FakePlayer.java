// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vsmecha.platform;

import com.github.litermc.vsmecha.Constants;
import com.github.litermc.vsmecha.util.IFakePlayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class FakePlayer extends net.fabricmc.fabric.api.entity.FakePlayer implements IFakePlayer {
	private static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(0, 0);

	private float destroySpeed = 1;
	private BlockState stateHasCorrectTool = null;

	private FakePlayer(ServerLevel serverLevel, GameProfile gameProfile) {
		super(serverLevel, gameProfile);
		this.setInvulnerable(true);
		this.refreshDimensions();
	}

	static FakePlayer create(ServerLevel serverLevel, GameProfile profile) {
		return new FakePlayer(serverLevel, profile);
	}

	@Override
	protected int getPermissionLevel() {
		return 0;
	}

	@Override
	public boolean broadcastToPlayer(ServerPlayer player) {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean isInvulnerable() {
		return true;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean isInvisible() {
		return false;
	}

	@Override
	public boolean isAffectedByPotions() {
		return false;
	}

	@Override
	public boolean attackable() {
		return false;
	}

	@Override
	public boolean canTakeItem(final ItemStack stack) {
		return false;
	}

	@Override
	public EntityDimensions getDimensions(final Pose pose) {
		return DIMENSIONS;
	}

	@Override
	public float getEyeHeight(final Pose pose) {
		return 0;
	}

	@Override
	public float getStandingEyeHeight(final Pose pose, final EntityDimensions dims) {
		return 0;
	}

	@Override
	public float getDestroySpeed(final BlockState state) {
		return this.hasCorrectToolForDrops(state) ? this.destroySpeed : Math.min(this.destroySpeed, 2);
	}

	@Override
	public boolean hasCorrectToolForDrops(final BlockState state) {
		return this.stateHasCorrectTool == state;
	}

	@Override
	public void setDestroySpeed(final float destroySpeed) {
		this.destroySpeed = destroySpeed;
	}

	@Override
	public void setHasCorrectToolForDrops(final BlockState stateHasCorrectTool) {
		this.stateHasCorrectTool = stateHasCorrectTool;
	}

	@Override
	public void tick() {
	}
}
