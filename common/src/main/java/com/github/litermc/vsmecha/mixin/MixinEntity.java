package com.github.litermc.vsmecha.mixin;

import com.github.litermc.vsmecha.block.control.CapsuleSeatBlockEntity;
import com.github.litermc.vsmecha.entity.SeatEntity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {
	@Shadow
	public abstract Entity getVehicle();

	@Unique
	private boolean hasFullLifeSupport() {
		if (!(this.getVehicle() instanceof SeatEntity seat)) {
			return false;
		}
		final CapsuleSeatBlockEntity be = seat.getAttachedBlock();
		if (be == null) {
			return false;
		}
		return be.hasFullLifeSupport();
	}

	@Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
	public void isInvulnerableTo(final DamageSource source, final CallbackInfoReturnable<Boolean> cir) {
		if (this.hasFullLifeSupport()) {
			cir.setReturnValue(true);
		}
	}
}
