package com.github.litermc.vsmecha.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import com.github.litermc.vsmecha.util.split.Splitter;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk extends ChunkAccess {
	protected MixinLevelChunk() {
		super(null, null, null, null, 0, null, null);
	}

	@Shadow
	@Final
	private Level level;

	@Inject(
		method = "setBlockState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;onRemove(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V"
		)
	)
	private void setBlockState(
		final BlockPos pos, final BlockState newState, final boolean moving,
		final CallbackInfoReturnable<BlockState> cir,
		final @Local(ordinal = 1) BlockState oldState
	) {
		if (!(this.level instanceof ServerLevel serverLevel)) {
			return;
		}

		Splitter.onBlockUpdated(serverLevel, pos, oldState, newState);
	}
}
