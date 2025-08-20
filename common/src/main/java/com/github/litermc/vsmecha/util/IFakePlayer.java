package com.github.litermc.vsmecha.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public interface IFakePlayer {
	static final UUID DEFAULT_PROFILE_UUID = UUID.fromString("0198c530-5df5-797a-a9e5-7b918801c96c");
	static final GameProfile DEFAULT_PROFILE = new GameProfile(DEFAULT_PROFILE_UUID, "[VSMecha]");

	void setDestroySpeed(float destroySpeed);

	void setHasCorrectToolForDrops(BlockState stateHasCorrectTool);
}
