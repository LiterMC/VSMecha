package com.github.litermc.vsmecha.util;

import com.github.litermc.vsmecha.platform.PlatformHelper;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

public final class DestroyUtil {
	public static final UUID DEFAULT_PROFILE_UUID = UUID.fromString("0198c530-5df5-797a-a9e5-7b918801c96c");
	public static final GameProfile DEFAULT_PROFILE = new GameProfile(DEFAULT_PROFILE_UUID, "[VSMecha]");

	public static void postTick(final ServerLevel level) {
		final DestroyData data = DestroyData.get(level);
		data.tick++;
		if (data.tick % 100 == 0) {
			refreshDestroyProgresses(level);
		}
	}

	public static boolean impact(final ServerLevel level, final BlockPos pos, final float speed) {
		return impact(level, pos, DEFAULT_PROFILE, speed);
	}

	public static boolean impact(final ServerLevel level, final BlockPos pos, GameProfile profile, final float speed) {
		if (profile == null) {
			profile = DEFAULT_PROFILE;
		}
		final ServerPlayer player = PlatformHelper.get().createFakePlayer(level, profile);
		((IFakePlayer) (player)).setDestroySpeed(speed);
		return impact(level, pos, player);
	}

	public static boolean impact(final ServerLevel level, final BlockPos pos, final ServerPlayer player) {
		if (!tryDestroy(level, pos, player)) {
			return false;
		}
		System.out.println("destroying: " + pos);
		level.destroyBlock(pos, true, player);
		return true;
	}

	private static boolean tryDestroy(final ServerLevel level, final BlockPos pos, final ServerPlayer player) {
		final BlockState state = level.getBlockState(pos);
		if (state.isAir()) {
			return false;
		}
		final float speed = state.getDestroySpeed(level, pos);
		if (speed == -1) {
			return false;
		}
		if (speed == 0) {
			return true;
		}
		final float inc = state.getDestroyProgress(player, level, pos);
		System.out.println("breaking: " + pos + " inc: " + inc);
		return addDestroyProgress(level, pos, inc);
	}

	public static boolean addDestroyProgress(final ServerLevel level, final BlockPos pos, final float inc) {
		if (!(inc >= 1e-4)) {
			return false;
		}
		final BlockState state = level.getBlockState(pos);
		if (state.isAir()) {
			return false;
		}
		final DestroyData data = DestroyData.get(level);
		final long longPos = pos.asLong();
		final float newProg = data.destroyProgress.addTo(longPos, inc) + inc;
		System.out.println("newProg: " + pos + ": " + newProg);
		data.recentlyActive.add(longPos);
		if (newProg < 1) {
			level.destroyBlockProgress(-1, pos, (int) (newProg * 10));
			return false;
		}
		level.destroyBlockProgress(-1, pos, -1);
		data.destroyProgress.remove(longPos);
		data.recentlyActive.remove(longPos);
		return true;
	}

	private static void refreshDestroyProgresses(final ServerLevel level) {
		final DestroyData data = DestroyData.get(level);
		for (final long pos : data.destroyProgress.keySet()) {
			if (!data.recentlyActive.contains(pos)) {
				level.destroyBlockProgress(-1, BlockPos.of(pos), -1);
				data.destroyProgress.remove(pos);
			}
		}
		final int oldSize = data.recentlyActive.size();
		data.recentlyActive.clear();
		data.recentlyActive.trim(oldSize);
	}

	private static final class DestroyData extends SavedData {
		private int tick = 0;
		private final Long2FloatOpenHashMap destroyProgress = new Long2FloatOpenHashMap();
		private final LongOpenHashSet recentlyActive = new LongOpenHashSet();

		static DestroyData get(final ServerLevel level) {
			return level.getDataStorage().computeIfAbsent((data) -> new DestroyData(), DestroyData::new, "VSMecha_DestroyData");
		}

		@Override
		public CompoundTag save(final CompoundTag data) {
			return data;
		}
	}
}
