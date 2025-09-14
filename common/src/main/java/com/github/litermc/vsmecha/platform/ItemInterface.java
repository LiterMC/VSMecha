package com.github.litermc.vsmecha.platform;

import net.minecraft.world.item.ItemStack;

public interface ItemInterface {
	int pushItem(ItemStack available, boolean simulate);
	int pullItem(ItemStack needs, boolean simulate);
}
