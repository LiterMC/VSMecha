package com.github.litermc.vsmecha.block.port;

import com.github.litermc.vsmecha.Constants;
import com.github.litermc.vsmecha.block.port.ItemPortBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.items.IItemHandler;

public final class ItemPortBlockEntityCapabilityProvider implements ICapabilityProvider {
	public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(Constants.MOD_ID, "item_port");

	private final ItemPortBlockEntity be;
	private final LazyOptional<IItemHandler> itemHandler;

	private ItemPortBlockEntityCapabilityProvider(final ItemPortBlockEntity be) {
		this.be = be;
		this.itemHandler = LazyOptional.of(() -> new ItemHandler(this.be));
	}

	@Override
	public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			return side == this.be.getDirection() ? LazyOptional.empty() : this.itemHandler.cast();
		}
		return LazyOptional.empty();
	}

	private void invalidate() {
		this.itemHandler.invalidate();
	}

	public static void onGatherCapabilities(final AttachCapabilitiesEvent<ItemPortBlockEntity> event) {
		final ItemPortBlockEntityCapabilityProvider provider = new ItemPortBlockEntityCapabilityProvider(event.getObject());
		event.addCapability(CAPABILITY_ID, provider);
		event.addListener(provider::invalidate);
	}

	private static final class ItemHandler implements IItemHandler {
		private final ItemPortBlockEntity be;

		private ItemHandler(final ItemPortBlockEntity be) {
			this.be = be;
		}

		@Override
		public int getSlots() {
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(final int slot) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(final int slot) {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean isItemValid(final int slot, final ItemStack stack) {
			return true;
		}

		@Override
		public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
			return stack.copyWithCount(stack.getCount() - this.be.pushItem(stack, simulate));
		}

		@Override
		public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
			return ItemStack.EMPTY;
		}
	}
}
