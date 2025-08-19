package com.github.litermc.vsaddontemplate.client;

import com.github.litermc.vsaddontemplate.VSAddonTemplateRegistry;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

public class ClientModEntry implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		VSAddonTemplateRegistry.Blocks.onRegisterRenderType(BlockRenderLayerMap.INSTANCE::putBlock);
	}
}
