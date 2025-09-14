package com.github.litermc.vsmecha.block.port;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.compat.computercraft.port.ItemPortPeripheral;
import com.github.litermc.vsmecha.platform.ItemInterface;
import com.github.litermc.vsmecha.platform.PlatformHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ItemPortBlockEntity extends AbstractPortBlockEntity<ItemStack> {
	public ItemPortBlockEntity(final BlockEntityType<? extends ItemPortBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public ItemPortBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.ITEM_PORT.get(), pos, state);
	}

	@Override
	public Object getPortType() {
		return ItemPortBlockEntity.class;
	}

	@Override
	protected Object createPeripheral() {
		return new ItemPortPeripheral(this);
	}

	@Override
	protected void onPortObjectOutput(final Context<ItemStack> context) {
		if (context.getAmount() <= 0) {
			return;
		}
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos outPos = this.getBlockPos().relative(this.getDirection());
		final ItemInterface ii = PlatformHelper.get().getItemInterface(level, outPos, this.getDirection().getOpposite());
		if (ii == null) {
			return;
		}
		final int pushed = ii.pushItem(context.getObject().copyWithCount(context.getAmount()), context.isSimulated());
		context.consume(pushed);
	}

	public int pushItem(final ItemStack stack, final boolean simulate) {
		if (stack.isEmpty()) {
			return 0;
		}
		final int amount = stack.getCount();
		final Context<ItemStack> context = new Context<>(stack, amount, simulate);
		this.onPortObjectInput(context);
		return amount - context.getAmount();
	}
}
