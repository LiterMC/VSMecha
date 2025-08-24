package com.github.litermc.vsmecha.block.energy;

import com.github.litermc.vsmecha.api.HeatAPI;
import com.github.litermc.vsmecha.block.BaseBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

public abstract class ThermalBasedBlockEntity extends BaseBlockEntity implements IThermalBlockEntity {
	private int heat = -1;

	protected ThermalBasedBlockEntity(final BlockEntityType<? extends ThermalBasedBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	@Override
	public int getHeat() {
		return this.heat;
	}

	@Override
	public void transferHeat(final int heat) {
		if (heat == 0) {
			return;
		}
		if (heat > 0) {
			if (this.heat >= Integer.MAX_VALUE - heat) {
				this.heat = Integer.MAX_VALUE - 1;
				return;
			}
		} else if (this.heat <= -heat) {
			this.heat = 0;
			return;
		}
		this.heat += heat;
		this.setChanged();
	}

	/**
	 * Block will instantly melt when its heat reach this point.
	 */
	public abstract int getMaxHeatCapacity();

	/**
	 * Block will become inefficient and will burn surrounding after this point.
	 */
	public abstract int getDangerousHeatLimit();

	public boolean isDangerous() {
		return this.getHeat() > this.gerDangerousHeatLimit();
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.heat = data.getInt("Heat");
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putInt("Heat", this.heat);
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		if (level instanceof ServerLevel serverLevel && this.heat == -1) {
			this.heat = HeatAPI.getBiomeHeat(serverLevel, this.getBlockPos());
		}
	}

	@Override
	public void serverTick() {
		super.serverTick();

		final ServerLevel level = (ServerLevel) (this.getLevel());

		long totalHeat = this.heat;
		final EnumMap<Direction, Integer> heats = new EnumMap<>();
		for (final Direction dir : Direction.values()) {
			final int nbHeat = HeatAPI.getBlockHeat(level, pos.relative(dir));
			if (nbHeat == Integer.MAX_VALUE) {
				continue;
			}
			totalHeat += nbHeat;
			heats.put(dir, nbHeat);
		}
		final long avgHeat = totalHeat / (1 + heats.size());
		for (final Direction dir : Direction.values()) {
			final Integer nbHeat = heats.get(dir);
			if (nbHeat == null) {
				continue;
			}
			final int diff = (int) ((avgHeat - nbHeat) * 2 / 10);
			this.transferHeat(-diff);
			if (level.getBlockEntity(pos) instanceof IThermalBlockEntity tbe) {
				tbe.transferHeat(diff);
			}
		}

		if (this.heat >= this.getMaxHeatCapacity()) {
			level.setBlock(this.getBlockPos(), Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 1), Block.UPDATE_ALL);
		}
	}
}
