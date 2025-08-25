package com.github.litermc.vsmecha.block.tool;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class StainedToolBlock extends ToolBaseBlock {
	private final DyeColor color;

	public StainedToolBlock(final DyeColor color, final BlockBehaviour.Properties props) {
		super(props);
		this.color = color;
	}

	public DyeColor getColor() {
		return this.color;
	}
}
