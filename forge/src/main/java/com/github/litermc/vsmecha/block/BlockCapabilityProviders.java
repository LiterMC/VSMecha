package com.github.litermc.vsmecha.block;

import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntityCapabilityProvider;
import com.github.litermc.vsmecha.block.port.FluidPortBlockEntity;
import com.github.litermc.vsmecha.block.port.FluidPortBlockEntityCapabilityProvider;
import com.github.litermc.vsmecha.block.port.ItemPortBlockEntity;
import com.github.litermc.vsmecha.block.port.ItemPortBlockEntityCapabilityProvider;
import com.github.litermc.vsmecha.compat.CompatMods;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;

public final class BlockCapabilityProviders {
	private BlockCapabilityProviders() {}

	public static void register() {
		MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, (AttachCapabilitiesEvent<? extends BlockEntity> event) -> {
			final BlockEntity be = event.getObject();
			if (CompatMods.COMPUTERCRAFT.isLoaded() && be instanceof IPeripheralBlockEntity) {
				IPeripheralBlockEntityCapabilityProvider.onGatherCapabilities((AttachCapabilitiesEvent<BlockEntity>) (event));
			}
			if (be instanceof EnergyBasedBlockEntity) {
				EnergyBasedBlockEntityCapabilityProvider.onGatherCapabilities((AttachCapabilitiesEvent<EnergyBasedBlockEntity>) (event));
			}
			if (be instanceof FluidPortBlockEntity) {
				FluidPortBlockEntityCapabilityProvider.onGatherCapabilities((AttachCapabilitiesEvent<FluidPortBlockEntity>) (event));
			}
			if (be instanceof ItemPortBlockEntity) {
				ItemPortBlockEntityCapabilityProvider.onGatherCapabilities((AttachCapabilitiesEvent<ItemPortBlockEntity>) (event));
			}
		});
	}
}
