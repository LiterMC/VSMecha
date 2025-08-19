package com.github.litermc.vsmecha.client;

import com.github.litermc.vsmecha.VSMechaRegistry;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

public class ClientModEntry implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		VSMechaRegistry.Blocks.onRegisterRenderType(BlockRenderLayerMap.INSTANCE::putBlock);
	}
}
