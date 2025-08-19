package com.github.litermc.vsaddontemplate.fabric.mixin;

import com.github.litermc.vsaddontemplate.platform.PlatformHelperImpl;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
	@Inject(
		method = "runServer",
		at = @At("HEAD")
	)
	private void runServer$head(final CallbackInfo ci) {
		PlatformHelperImpl.minecraftServer = ((MinecraftServer) ((Object) (this)));
	}

	@Inject(
		method = "runServer",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;onServerExit()V")
	)
	private void runServer$exit(final CallbackInfo ci) {
		PlatformHelperImpl.minecraftServer = null;
	}
}
