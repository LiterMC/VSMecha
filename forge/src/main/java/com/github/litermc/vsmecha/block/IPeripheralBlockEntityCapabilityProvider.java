package com.github.litermc.vsmecha.block;

import com.github.litermc.vsmecha.Constants;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import dan200.computercraft.shared.Capabilities;

public final class IPeripheralBlockEntityCapabilityProvider implements ICapabilityProvider {
	public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(Constants.MOD_ID, "peripheral");

	private final BlockEntity be;
	private final LazyOptional<Object> lazyPeripheral;

	private IPeripheralBlockEntityCapabilityProvider(final BlockEntity be) {
		this.be = be;
		this.lazyPeripheral = LazyOptional.of(((IPeripheralBlockEntity) (be))::getOrCreatePeripheral);
	}

	@Override
	public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side) {
		if (cap == Capabilities.CAPABILITY_PERIPHERAL) {
			return this.lazyPeripheral.cast();
		}
		return LazyOptional.empty();
	}

	private void invalidate() {
		this.lazyPeripheral.invalidate();
	}

	public static void onGatherCapabilities(final AttachCapabilitiesEvent<BlockEntity> event) {
		final IPeripheralBlockEntityCapabilityProvider provider = new IPeripheralBlockEntityCapabilityProvider(event.getObject());
		event.addCapability(CAPABILITY_ID, provider);
		event.addListener(provider::invalidate);
	}
}
