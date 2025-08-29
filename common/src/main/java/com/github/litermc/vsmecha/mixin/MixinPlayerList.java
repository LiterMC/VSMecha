package com.github.litermc.vsmecha.mixin;

import com.github.litermc.vsmecha.accessor.PlayerListAccessor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerList.class)
public abstract class MixinPlayerList implements PlayerListAccessor {
	@Shadow
	protected abstract void save(ServerPlayer player);

	@Override
	public void vsm$save(final ServerPlayer player) {
		this.save(player);
	}
}
