package com.github.litermc.vsmecha.mixin.create;

import com.github.litermc.vsmecha.accessor.ControlledContraptionEntityAccessor;

import net.minecraft.core.BlockPos;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(ControlledContraptionEntity.class)
public class MixinControlledContraptionEntity implements ControlledContraptionEntityAccessor {
	@Shadow(remap = false)
	protected BlockPos controllerPos;

	@Override
	public BlockPos vsmecha$getControllerPos() {
		return this.controllerPos;
	}

	@Override
	public void vsmecha$setControllerPos(BlockPos pos) {
		this.controllerPos = pos;
	}
}
