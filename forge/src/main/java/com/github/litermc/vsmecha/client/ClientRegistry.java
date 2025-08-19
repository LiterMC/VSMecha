package com.github.litermc.vsmecha.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.github.litermc.vsmecha.Constants;
import com.github.litermc.vsmecha.VSMechaRegistry;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientRegistry {
	@SubscribeEvent
	public static void clientSetup(final FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			VSMechaRegistry.Blocks.onRegisterRenderType(ItemBlockRenderTypes::setRenderLayer);
		});
	}
}
