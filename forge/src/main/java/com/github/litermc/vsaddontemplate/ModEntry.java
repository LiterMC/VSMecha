package com.github.litermc.vsaddontemplate;

import com.github.litermc.vsaddontemplate.block.BlockCapabilityProviders;
import com.github.litermc.vsaddontemplate.command.VSAddonTemplateCommands;
import com.github.litermc.vsaddontemplate.config.ConfigSpec;
import com.github.litermc.vsaddontemplate.platform.ForgeConfigFile;

import com.electronwill.nightconfig.core.file.FileConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
@Mod.EventBusSubscriber
public class ModEntry {
	public ModEntry() {
		final FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
		final IEventBus modBus = context.getModEventBus();

		VSAddonTemplateRegistry.register();
		BlockCapabilityProviders.register();

		context.registerConfig(ModConfig.Type.SERVER, ((ForgeConfigFile)(ConfigSpec.serverSpec)).spec());
		modBus.addListener(this::onConfigLoad);
		modBus.addListener(this::onConfigReload);
	}

	@SubscribeEvent
	public static void onLevelLoad(final LevelEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel level) {
			VSAddonTemplateListeners.onServerLevelLoad(level);
		}
	}

	@SubscribeEvent
	public static void onLevelUnload(final LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel level) {
			VSAddonTemplateListeners.onServerLevelUnload(level);
		}
	}

	@SubscribeEvent
	public static void onServerTick(final TickEvent.ServerTickEvent event) {
		switch (event.phase) {
		case START -> VSAddonTemplateListeners.preServerTick(event.getServer());
		case END -> VSAddonTemplateListeners.postServerTick(event.getServer());
		}
	}

	@SubscribeEvent
	public static void onRegisterCommands(final RegisterCommandsEvent event) {
		VSAddonTemplateCommands.register(event.getDispatcher());
	}

	// Following code comes from CC: Tweaked
	//
	// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
	//
	// SPDX-License-Identifier: MPL-2.0

	private void onConfigLoad(final ModConfigEvent.Loading event) {
		this.syncConfig(event.getConfig());
	}

	private void onConfigReload(final ModConfigEvent.Reloading event) {
		this.syncConfig(event.getConfig());
	}

	private void syncConfig(final ModConfig config) {
		if (!config.getModId().equals(Constants.MOD_ID)) return;

		var path = config.getConfigData() instanceof FileConfig fileConfig ? fileConfig.getNioPath() : null;

		if (config.getType() == ModConfig.Type.SERVER && ((ForgeConfigFile)(ConfigSpec.serverSpec)).spec().isLoaded()) {
			ConfigSpec.syncServer(path);
		} else if (config.getType() == ModConfig.Type.CLIENT) {
			ConfigSpec.syncClient(path);
		}
	}
}
