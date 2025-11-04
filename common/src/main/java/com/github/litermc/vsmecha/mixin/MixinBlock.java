package com.github.litermc.vsmecha.mixin;

import com.github.litermc.vsmecha.util.split.anchor.NeighbourBlockAnchor;

import net.minecraft.world.level.block.Block;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public abstract class MixinBlock implements NeighbourBlockAnchor {
}
