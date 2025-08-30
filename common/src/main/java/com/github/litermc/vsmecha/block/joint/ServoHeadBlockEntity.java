package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.block.BaseBlockEntity;
import com.github.litermc.vsmecha.block.IPeripheralBlockEntity;
import com.github.litermc.vsmecha.compat.CompatMods;
import com.github.litermc.vsmecha.compat.computercraft.ServoHeadPeripheral;
import com.github.litermc.vsmecha.compat.computercraft.network.ShipModemPeripheral;
import com.github.litermc.vsmecha.util.ShipUtil;
import com.github.litermc.vsmecha.util.TaskUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemLocalPeripheral;

import java.util.HashMap;
import java.util.Map;

public class ServoHeadBlockEntity extends BaseBlockEntity implements IJointBlockEntity, IPeripheralBlockEntity {
	private final Direction direction;
	BlockPos basePos = null;
	ServoBlockEntity.ServoInfo servoInfo = null;

	private Object /*IPeripheral*/ peripheral = null;
	private Object /*ShipModemPeripheral*/ modemPeripheral = null;

	public ServoHeadBlockEntity(final BlockEntityType<? extends ServoHeadBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.direction = state.getValue(BlockStateProperties.FACING);
	}

	public ServoHeadBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSMechaRegistry.BlockEntities.SERVO_HEAD.get(), pos, state);
	}

	public Direction getDirection() {
		return this.direction;
	}

	@Override
	public BlockPos getAttachingBlock() {
		return this.basePos;
	}

	@Override
	public ServerShip getPeerShip() {
		if (this.basePos == null) {
			return null;
		}
		return ShipUtil.getServerShip((ServerLevel) (this.getLevel()), this.basePos);
	}

	@Override
	public boolean canTransferEnergy() {
		return this.basePos != null && this.getLevel().getBlockEntity(this.basePos) instanceof ServoBlockEntity sbe && sbe.canTransferEnergy();
	}

	@Override
	public final Object getOrCreatePeripheral() {
		if (this.peripheral == null) {
			this.peripheral = new ServoHeadPeripheral(this);
		}
		return this.peripheral;
	}

	@Override
	public final Object getShipModemPeripheral() {
		return this.modemPeripheral;
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		if (!level.isClientSide && CompatMods.COMPUTERCRAFT.isLoaded() && VSGameUtilsKt.isBlockInShipyard(level, this.getBlockPos())) {
			TaskUtil.queueTickEnd(() -> {
				final ShipModemPeripheral modemPeripheral = new ShipModemPeripheral(this);
				this.modemPeripheral = modemPeripheral;
				final WiredModemLocalPeripheral localPeripheral = modemPeripheral.getLocalPeripheral();
				localPeripheral.attach(level, this.getBlockPos().above(), Direction.DOWN);
				final Map<String, IPeripheral> peripheralMap = new HashMap<>();
				localPeripheral.extendMap(peripheralMap);
				modemPeripheral.getElement().getNode().updatePeripherals(peripheralMap);
			});
		}
	}

	@Override
	public void serverTick() {
		super.serverTick();

		if (this.basePos == null) {
			return;
		}
		if (this.servoInfo.detached) {
			this.basePos = null;
			this.servoInfo = null;
			return;
		}

		final ServerLevel level = (ServerLevel) (this.getLevel());
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(level);
		if (level.getBlockEntity(this.basePos) instanceof ServoBlockEntity sbe && sbe.servoInfo == this.servoInfo) {
			return;
		}
		this.servoInfo.detach(world);
		this.servoInfo.detached = true;
		this.servoInfo = null;
		this.basePos = null;
	}
}
