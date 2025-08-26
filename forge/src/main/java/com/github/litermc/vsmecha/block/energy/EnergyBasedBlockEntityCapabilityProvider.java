package com.github.litermc.vsmecha.block.energy;

import com.github.litermc.vsmecha.Constants;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import org.valkyrienskies.mod.common.VSGameUtilsKt;

public final class EnergyBasedBlockEntityCapabilityProvider implements ICapabilityProvider {
	public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(Constants.MOD_ID, "energy_based");

	private final EnergyBasedBlockEntity be;
	private final LazyOptional<IEnergyStorage> energyStorage;

	private EnergyBasedBlockEntityCapabilityProvider(final EnergyBasedBlockEntity be) {
		this.be = be;
		this.energyStorage = LazyOptional.of(() -> new EnergyStorage(this.be));
	}

	@Override
	public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side) {
		if (cap == ForgeCapabilities.ENERGY) {
			return this.energyStorage.cast();
		}
		return LazyOptional.empty();
	}

	private void invalidate() {
		this.energyStorage.invalidate();
	}

	public static void onGatherCapabilities(final AttachCapabilitiesEvent<EnergyBasedBlockEntity> event) {
		final EnergyBasedBlockEntityCapabilityProvider provider = new EnergyBasedBlockEntityCapabilityProvider(event.getObject());
		event.addCapability(CAPABILITY_ID, provider);
		event.addListener(provider::invalidate);
	}

	private static final class EnergyStorage implements IEnergyStorage {
		private final EnergyBasedBlockEntity be;
		private final boolean isOnShip;
		private final boolean isPort;

		private EnergyStorage(final EnergyBasedBlockEntity be) {
			this.be = be;
			this.isOnShip = VSGameUtilsKt.isBlockInShipyard(be.getLevel(), be.getBlockPos());
			this.isPort = this.be instanceof EnergyPortBlockEntity;
		}

		public int receiveEnergy(final int maxReceive, final boolean simulate) {
			if (!this.canReceive() || !this.be.isEnabled()) {
				return 0;
			}
			final int energy = this.be.getEnergyStored();
			final int received = Math.min(
				Math.min(this.be.getMaxEnergyStorage() - energy, maxReceive),
				this.isOnShip && !this.isPort ? this.be.energyInputRemaining : Integer.MAX_VALUE
			);
			if (!simulate) {
				this.be.energyInputRemaining -= received;
				this.be.setEnergyStored(energy + received);
			}
			return received;
		}

		public int extractEnergy(final int maxExtract, final boolean simulate) {
			if (!this.canExtract() || !this.be.isEnabled()) {
				return 0;
			}
			final int energy = this.be.getEnergyStored();
			final int extracted = Math.min(Math.min(energy, maxExtract), this.isOnShip ? this.be.energyOutputRemaining : Integer.MAX_VALUE);
			if (!simulate) {
				this.be.energyOutputRemaining -= extracted;
				this.be.setEnergyStored(energy - extracted);
			}
			return extracted;
		}

		public int getEnergyStored() {
			return this.be.getEnergyStored();
		}

		public int getMaxEnergyStored() {
			return this.be.getMaxEnergyStorage();
		}

		public boolean canExtract() {
			return this.be.canPullByExternal() && (this.isPort || this.be.getEnergyOutputLimit() != 0);
		}

		public boolean canReceive() {
			return this.be.canPushByExternal() && (this.isPort || this.be.getEnergyInputLimit() != 0);
		}
	}
}
