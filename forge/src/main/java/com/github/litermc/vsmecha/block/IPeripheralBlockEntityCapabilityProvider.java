package com.github.litermc.vsmecha.block;

import com.github.litermc.vsmecha.Constants;
import com.github.litermc.vsmecha.block.IJointPeripheralBlockEntity;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipModemPeripheral;

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

	private final IPeripheralBlockEntity be;
	private final LazyOptional<Object> lazyPeripheral;
	private LazyOptional<Object> lazyWireElement = LazyOptional.empty();

	private IPeripheralBlockEntityCapabilityProvider(final BlockEntity be) {
		this.be = (IPeripheralBlockEntity) (be);
		this.lazyPeripheral = LazyOptional.of(() -> this.be.getShipPeripheralHolder().getOrCreatePeripheral());
	}

	@Override
	public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side) {
		if (cap == Capabilities.CAPABILITY_PERIPHERAL) {
			return this.lazyPeripheral.cast();
		}
		if (cap == Capabilities.CAPABILITY_WIRED_ELEMENT) {
			if (this.be instanceof IJointPeripheralBlockEntity jbe && jbe.canConnectPeripheralWire(side)) {
				if (!this.lazyWireElement.isPresent()) {
					final ShipModemPeripheral modem = this.be.getShipPeripheralHolder().getShipModemPeripheral();
					if (modem != null) {
						this.lazyWireElement = LazyOptional.of(modem::getElement);
					}
				}
				return this.lazyWireElement.cast();
			}
			return LazyOptional.empty();
		}
		return LazyOptional.empty();
	}

	private void invalidate() {
		this.lazyPeripheral.invalidate();
		this.lazyWireElement.invalidate();
	}

	public static void onGatherCapabilities(final AttachCapabilitiesEvent<BlockEntity> event) {
		final IPeripheralBlockEntityCapabilityProvider provider = new IPeripheralBlockEntityCapabilityProvider(event.getObject());
		event.addCapability(CAPABILITY_ID, provider);
		event.addListener(provider::invalidate);
	}
}
