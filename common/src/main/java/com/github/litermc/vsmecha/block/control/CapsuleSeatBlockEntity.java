package com.github.litermc.vsmecha.block.control;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.entity.SeatEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.UUID;

public class CapsuleSeatBlockEntity extends EnergyBasedBlockEntity {
	private static final int STANDARD_HEAT = 10000;
	private static final int LIFE_SUPPORT_USE = 1000;
	private final Direction direction;
	private boolean lifeSupportEnabled = true;

	private Player player = null;
	private UUID playerUUID = null; 
	private SeatEntity seatEntity = null;
	private UUID seatUUID = null;

	public CapsuleSeatBlockEntity(final BlockEntityType<? extends CapsuleSeatBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.direction = state.getValue(BlockStateProperties.FACING);
	}

	public CapsuleSeatBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.CAPSULE_SEAT.get(), pos, state);
	}

	public Direction getDirection() {
		return this.direction;
	}

	public int getMaxHeatAdjustRate() {
		return 5780;
	}

	public boolean isLifeSupportEnabled() {
		return this.lifeSupportEnabled;
	}

	public void setLifeSupportEnabled(final boolean lifeSupportEnabled) {
		this.lifeSupportEnabled = lifeSupportEnabled;
	}

	public Player getPlayer() {
		return this.player;
	}

	public UUID getPlayerUUID() {
		return this.playerUUID;
	}

	public SeatEntity getSeatEntity() {
		if (this.seatEntity == null && this.seatUUID != null) {
			if (((ServerLevel) (this.getLevel())).getEntity(this.seatUUID) instanceof SeatEntity seatEntity) {
				this.seatEntity = seatEntity;
			} else {
				this.seatUUID = null;
			}
		}
		return this.seatEntity;
	}

	public void removeSeatEntity() {
		if (this.seatUUID == null) {
			return;
		}
		this.seatUUID = null;
		this.setChanged();
		if (this.seatEntity == null) {
			return;
		}
		this.seatEntity.discard();
		this.seatEntity = null;
	}

	public SeatEntity getOrCreateSeatEntity() {
		final SeatEntity entity = this.getSeatEntity();
		if (entity != null) {
			return entity;
		}
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final SeatEntity newEntity = new SeatEntity(VSMechaRegistry.Entities.SEAT.get(), level);
		level.addFreshEntity(newEntity);
		this.setChanged();
		return newEntity;
	}

	public void setPlayer(final Player player) {
		if (this.player != player) {
			return;
		}
		this.player = player;
		this.playerUUID = player.getUUID();
		this.setChanged();
	}

	public boolean hasCapsuleHead() {
		return this.getLevel().getBlockState(this.getBlockPos().above()).getBlock() instanceof CapsuleHeadBlock;
	}

	public boolean hasFullLifeSupport() {
		return this.isEnabled() && this.isLifeSupportEnabled() && this.hasCapsuleHead() && this.getEnergyStored() >= LIFE_SUPPORT_USE;
	}

	@Override
	public int getMaxHeatCapacity() {
		return 21000;
	}

	@Override
	public int getDangerousHeatLimit() {
		return 20000;
	}

	@Override
	public int getDefaultEnergyPriority() {
		return 1000;
	}

	@Override
	public int getMaxEnergyStorage() {
		return 100000;
	}

	@Override	
	public int getEnergyInputLimit() {
		return this.getMaxEnergyStorage();
	}

	@Override
	public int getEnergyOutputLimit() {
		return 0;
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.lifeSupportEnabled = data.getBoolean("LifeSupport");
		if (data.contains("Player")) {
			this.playerUUID = data.getUUID("Player");
		} else {
			this.player = null;
			this.playerUUID = null;
		}
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putBoolean("LifeSupport", this.lifeSupportEnabled);
	}

	@Override
	protected void saveShared(final CompoundTag data) {
		super.saveShared(data);
		if (this.playerUUID != null) {
			data.putUUID("Player", this.playerUUID);
		}
	}

	@Override
	public void serverTick() {
		if (this.isEnabled()) {
			final int energy = this.getEnergyStored();
			if (this.isLifeSupportEnabled() && energy >= LIFE_SUPPORT_USE) {
				this.setEnergyStored(energy - LIFE_SUPPORT_USE);
				this.tickLifeSupport();
			}
		}
		super.serverTick();
	}

	protected void tickLifeSupport() {
		final int diff = STANDARD_HEAT - this.getHeat();
		final int maxAdjust = this.getMaxHeatAdjustRate();
		this.transferHeat(diff < 0 ? Math.max(diff, -maxAdjust) : Math.min(diff, maxAdjust));

		final Entity seatEntity = this.seatEntity;
		if (seatEntity != null) {
			final Entity passenger = seatEntity.getFirstPassenger();
			if (passenger != null) {
				this.tickLifeSupportOnPassenger(passenger);
			}
		}
	}

	protected void tickLifeSupportOnPassenger(final Entity entity) {
		entity.clearFire();
		entity.setAirSupply(entity.getMaxAirSupply());
		entity.setTicksFrozen(0);
	}
}
