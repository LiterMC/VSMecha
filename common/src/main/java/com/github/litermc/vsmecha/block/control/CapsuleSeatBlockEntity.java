package com.github.litermc.vsmecha.block.control;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.accessor.PlayerListAccessor;
import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.compat.computercraft.CapsuleSeatPeripheral;
import com.github.litermc.vsmecha.entity.SeatEntity;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import java.util.UUID;

public class CapsuleSeatBlockEntity extends EnergyBasedBlockEntity {
	private static final int STANDARD_HEAT = 10000;
	private static final int LIFE_SUPPORT_USE = 1000;
	private final Direction direction;
	private volatile boolean lifeSupportEnabled = true;

	private SeatEntity seatEntity = null;
	private UUID seatUUID = null;
	private volatile UUID playerUUID = null; 

	private int ticks = 0;

	public CapsuleSeatBlockEntity(final BlockEntityType<? extends CapsuleSeatBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
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
		if (this.lifeSupportEnabled == lifeSupportEnabled) {
			return;
		}
		this.lifeSupportEnabled = lifeSupportEnabled;
		this.setChanged();
	}

	public UUID getPlayerUUID() {
		return this.playerUUID;
	}

	public SeatEntity getSeatEntity() {
		if (this.seatEntity == null && this.seatUUID != null) {
			final Entity entity = ((ServerLevel) (this.getLevel())).getEntity(this.seatUUID);
			if (entity == null && this.playerUUID != null) {
				return null;
			}
			if (entity instanceof SeatEntity seatEntity) {
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

	private SeatEntity getOrCreateSeatEntity() {
		final SeatEntity entity = this.getSeatEntity();
		if (this.playerUUID != null || entity != null) {
			return entity;
		}
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos pos = this.getBlockPos();
		final SeatEntity newEntity = new SeatEntity(VSMechaRegistry.Entities.SEAT.get(), level);
		newEntity.setAttachedBlockPos(pos);
		newEntity.setPos(pos.getCenter().add(0, -0.3, 0));
		this.seatEntity = newEntity;
		this.seatUUID = newEntity.getUUID();
		level.addFreshEntity(newEntity);
		this.setChanged();
		return newEntity;
	}

	public void setPlayer(final Player player) {
		final UUID uuid = player == null ? null : player.getUUID();
		if (this.playerUUID == uuid) {
			return;
		}
		this.playerUUID = uuid;
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
	protected Object createPeripheral() {
		return new CapsuleSeatPeripheral(this);
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.lifeSupportEnabled = data.getBoolean("LifeSupport");
		if (data.contains("Player")) {
			this.playerUUID = data.getUUID("Player");
		} else {
			this.playerUUID = null;
		}
		if (data.contains("Seat")) {
			this.seatUUID = data.getUUID("Seat");
		}
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putBoolean("LifeSupport", this.lifeSupportEnabled);
		if (this.seatUUID != null) {
			data.putUUID("Seat", this.seatUUID);
		}
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
		final ServerLevel level = (ServerLevel) (this.getLevel());
		if (this.seatEntity != null) {
			if (this.seatEntity.isRemoved()) {
				final Entity.RemovalReason reason = this.seatEntity.getRemovalReason();
				if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
					this.seatEntity = null;
				} else {
					this.seatEntity = null;
					this.seatUUID = null;
					this.setChanged();
				}
			} else if (this.playerUUID != null) {
				final Entity player = level.getEntity(this.playerUUID);
				if ((player == null || !this.seatEntity.hasPassenger(player))) {
					this.playerUUID = null;
					this.setChanged();
				}
			}
		}
		if (this.isEnabled()) {
			final int energy = this.getEnergyStored();
			if (this.isLifeSupportEnabled() && energy >= LIFE_SUPPORT_USE) {
				this.setEnergyStored(energy - LIFE_SUPPORT_USE);
				this.tickLifeSupport();
			}
		}
		super.serverTick();
	}

	public boolean onUse(final Player player, final BlockHitResult hit) {
		if (this.playerUUID != null) {
			if (player.getUUID().equals(this.playerUUID)) {
				return this.onSeatedUse(player, hit);
			}
			return false;
		}
		if (!(this.getLevel() instanceof ServerLevel level)) {
			return true;
		}
		final SeatEntity seat = this.getOrCreateSeatEntity();
		if (seat == null || !player.startRiding(seat)) {
			return false;
		}
		this.setPlayer(player);
		return true;
	}

	public boolean onSeatedUse(final Player player, final BlockHitResult hit) {

		return true;
	}

	@Override
	public void beforeRemove() {
		if (!(this.getLevel() instanceof ServerLevel level)) {
			return;
		}
		final Entity seatEntity = this.getSeatEntity();
		this.seatUUID = null;
		this.setChanged();
		if (seatEntity != null) {
			seatEntity.discard();
			return;
		}
		if (this.playerUUID == null || level.getEntity(this.playerUUID) != null) {
			return;
		}
		final MinecraftServer server = level.getServer();

		final GameProfile profile = server.getProfileCache().get(this.playerUUID).orElse(null);
		this.playerUUID = null;
		if (profile == null) {
			return;
		}
		final PlayerList playerList = server.getPlayerList();
		final ServerPlayer player = playerList.getPlayerForLogin(profile);
		final CompoundTag playerData = playerList.load(player);
		if (player.isSpectator() || player.isCreative()) {
			return;
		}
		player.setServerLevel(level);
		player.loadGameTypes(playerData);
		new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.CLIENTBOUND), player) {
			@Override
			public void send(final Packet<?> packet) {}
		};

		final Entity killer = null; // TODO
		player.setHealth(0);
		player.die(player.damageSources().explosion(killer, killer));
		player.unRide();
		((PlayerListAccessor)(playerList)).vsm$save(player);
	}

	protected void tickLifeSupport() {
		final int diff = STANDARD_HEAT - this.getHeat();
		final int maxAdjust = this.getMaxHeatAdjustRate();
		this.transferHeat(diff < 0 ? Math.max(diff, -maxAdjust) : Math.min(diff, maxAdjust));

		final Entity seatEntity = this.getSeatEntity();
		if (seatEntity != null) {
			final Entity passenger = seatEntity.getFirstPassenger();
			if (passenger != null && (!(passenger instanceof LivingEntity livingEntity) || !livingEntity.isDeadOrDying())) {
				this.tickLifeSupportOnPassenger(passenger);
			}
		}
	}

	protected void tickLifeSupportOnPassenger(final Entity entity) {
		entity.clearFire();
		entity.setAirSupply(entity.getMaxAirSupply());
		entity.setTicksFrozen(0);
		this.ticks++;
		if (this.ticks % 5 == 0) {
			if (entity instanceof LivingEntity livingEntity) {
				livingEntity.setHealth(livingEntity.getHealth() + 1);
			}
			if (entity instanceof Player player) {
				player.getFoodData().eat(20, 20);
			}
		}
	}
}
