package com.github.litermc.vsmecha.block.energy;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.platform.EnergyInterface;
import com.github.litermc.vsmecha.platform.PlatformHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class EnergyPortBlockEntity extends EnergyBasedBlockEntity {
	private final Direction direction;
	private int transferred = 0;

	public EnergyPortBlockEntity(final BlockEntityType<? extends EnergyPortBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.direction = state.getValue(BlockStateProperties.FACING);
	}

	public EnergyPortBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.ENERGY_PORT.get(), pos, state);
	}

	@Override
	public int getMaxHeatCapacity() {
		return 13000;
	}

	@Override
	public int getDangerousHeatLimit() {
		return 12000;
	}

	@Override
	public int getDefaultEnergyPriority() {
		return -100;
	}

	@Override
	public int getMaxEnergyStorage() {
		return 10000;
	}

	@Override	
	public int getEnergyInputLimit() {
		return 0;
	}

	@Override
	public int getEnergyOutputLimit() {
		return this.getMaxEnergyStorage();
	}

	@Override
	public void consumeEnergy(final int amount) {
		super.consumeEnergy(amount);
		this.transferred += amount;
	}

	@Override
	public int tickEnergyInput(final int available) {
		if (!this.isEnabled()) {
			return 0;
		}
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos outPos = this.getBlockPos().relative(this.direction);
		final EnergyInterface ei = PlatformHelper.get().getEnergyInterface(level, outPos, this.direction.getOpposite());
		if (ei == null) {
			return 0;
		}
		final int pushed = ei.pushEnergy(Math.min(available, 10000), false);
		this.transferred += pushed;
		return pushed;
	}

	@Override
	public void serverTick() {
		super.serverTick();

		this.transferHeat(this.transferred / 10000);
		this.transferred = 0;
	}
}
