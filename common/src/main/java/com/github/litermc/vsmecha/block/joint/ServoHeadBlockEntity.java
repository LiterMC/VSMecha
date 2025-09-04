package com.github.litermc.vsmecha.block.joint;

import com.github.litermc.vsmecha.VSMechaRegistry;
import com.github.litermc.vsmecha.attachment.ShipNetworkAttachment;
import com.github.litermc.vsmecha.block.BaseBlockEntity;
import com.github.litermc.vsmecha.block.IJointPeripheralBlockEntity;
import com.github.litermc.vsmecha.block.IPhysTickableBlockEntity;
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

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemLocalPeripheral;
import dan200.computercraft.shared.platform.ComponentAccess;
import dan200.computercraft.shared.platform.PlatformHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ServoHeadBlockEntity extends BaseBlockEntity implements IJointBlockEntity, IJointPeripheralBlockEntity, IPhysTickableBlockEntity {
	private final Direction direction;
	BlockPos basePos = null;
	private volatile ServoBlockEntity sbe = null;
	ServoBlockEntity.ServoInfo servoInfo = null;

	private Object /*IPeripheral*/ peripheral = null;
	private Object /*ShipModemPeripheral*/ modemPeripheral = null;
	private final Object /*ComponentAccess<WiredElement>*/ cableAccess = CompatMods.COMPUTERCRAFT.isLoaded()
		? PlatformHelper.get().createWiredElementAccess(this, (side) -> this.queueRefreshCables())
		: null;
	private volatile boolean refreshingCables = false;

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
	public boolean canConnectPeripheralWire(final Direction dir) {
		return this.direction.getOpposite() == dir;
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		if (!(level instanceof ServerLevel serverLevel) || !CompatMods.COMPUTERCRAFT.isLoaded()) {
			return;
		}
		final BlockPos pos = this.getBlockPos();
		final ShipModemPeripheral modemPeripheral = new ShipModemPeripheral(this);
		this.modemPeripheral = modemPeripheral;
		final WiredModemLocalPeripheral localPeripheral = modemPeripheral.getLocalPeripheral();
		TaskUtil.queueTickEnd(() -> {
			localPeripheral.attach(serverLevel, pos.above(), Direction.DOWN);
			final Map<String, IPeripheral> peripheralMap = new HashMap<>();
			localPeripheral.extendMap(peripheralMap);
			modemPeripheral.getElement().getNode().updatePeripherals(peripheralMap);
		});
		if (this instanceof IJointPeripheralBlockEntity) {
			this.queueRefreshCables();
		}
		final ServerShip ship = ShipUtil.getServerShip(serverLevel, pos);
		if (ship instanceof final LoadedServerShip loadedShip) {
			ShipNetworkAttachment.get(loadedShip).registerPeripheral(this);
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (CompatMods.COMPUTERCRAFT.isLoaded() && this.modemPeripheral instanceof ShipModemPeripheral modem) {
			modem.getElement().getNode().remove();
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
		if (level.getBlockEntity(this.basePos) instanceof final ServoBlockEntity sbe && sbe.servoInfo == this.servoInfo) {
			this.sbe = sbe;
			return;
		}
		this.sbe = null;
		this.servoInfo.detach(world);
		this.servoInfo.detached = true;
		this.servoInfo = null;
		this.basePos = null;
	}

	@Override
	public void physicsTick(final PhysShip ship, final Function<Long, PhysShip> lookup) {
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final BlockPos basePos = this.basePos;
		if (basePos == null) {
			return;
		}
		final ServerShip peerShip = VSGameUtilsKt.getShipManagingPos(level, basePos);
		if (peerShip != null && !peerShip.isStatic()) {
			return;
		}
		final ServoBlockEntity sbe = this.sbe;
		if (sbe == null || sbe.isRemoved()) {
			return;
		}
		sbe.stepServo(peerShip, ship, 1.0 / 60);
	}

	private void queueRefreshCables() {
		if (this.refreshingCables) {
			return;
		}
		this.refreshingCables = true;
		TaskUtil.queueTickEnd(this::refreshCables);
	}

	private void refreshCables() {
		this.refreshingCables = false;
		final WiredNode node = ((ShipModemPeripheral) (this.modemPeripheral)).getElement().getNode();
		final ComponentAccess<WiredElement> cableAccess = (ComponentAccess<WiredElement>) (this.cableAccess);
		for (final Direction dir : Direction.values()) {
			final WiredElement element = cableAccess.get(dir);
			if (element == null) {
				continue;
			}
			if (this.canConnectPeripheralWire(dir)) {
				node.connectTo(element.getNode());
			} else {
				node.disconnectFrom(element.getNode());
			}
		}
	}
}
