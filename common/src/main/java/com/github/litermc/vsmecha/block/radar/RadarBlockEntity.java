package com.github.litermc.vsmecha.block.radar;

import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.compat.CompatMods;
import com.github.litermc.vsmecha.compat.computercraft.radar.RadarPeripheral;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RadarBlockEntity extends EnergyBasedBlockEntity {
	private volatile int radius = 0;
	private volatile int scanRemaning = 0;
	private volatile boolean autoScan = false;
	private volatile boolean queuingScan = false;

	private int radarSignatureStrength = 0;

	protected RadarBlockEntity(final BlockEntityType<? extends RadarBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	@Override
	public int getMaxHeatCapacity() {
		return 18000;
	}

	@Override
	public int getDangerousHeatLimit() {
		return 12000;
	}

	@Override
	public int getDefaultEnergyPriority() {
		return 500;
	}

	@Override
	public int getEnergyOutputLimit() {
		return 0;
	}

	public int getScanRadius() {
		return this.radius;
	}

	public void setScanRadius(final int radius) {
		if (this.radius == radius) {
			return;
		}
		this.radius = radius;
		this.setChanged();
	}

	/**
	 * @return Energy needs to perform next scan, {@code -1} means scan is impossible.
	 */
	public abstract int getEnergyConsumption();

	/**
	 * @return Time in ticks needs to perform next scan.
	 */
	public abstract int getScanTime();

	public boolean getAutoScan() {
		return this.autoScan;
	}

	public void setAutoScan(final boolean autoScan) {
		if (this.autoScan == autoScan) {
			return;
		}
		this.autoScan = autoScan;
		this.setChanged();
	}

	/**
	 * Vehicle that has stronger radar signature is easier to detect by specific radars.
	 *
	 * @return Radar signature strength.
	 */
	public int getRadarSignatureStrength() {
		return this.radarSignatureStrength;
	}

	protected void increaseRadarSignatureStrength(final int inc) {
		this.radarSignatureStrength += inc;
	}

	public boolean isScanning() {
		return this.scanRemaning > 0;
	}

	public void queueScan() {
		this.queuingScan = true;
	}

	protected boolean prepareScan() {
		return true;
	}

	protected abstract void finalizeScan(List<ScanResult> results);

	protected void onScanFinished(final List<ScanResult> results) {
		// TODO: usage without CC?
		if (CompatMods.COMPUTERCRAFT.isLoaded() && this.getPeripheral() instanceof final RadarPeripheral peripheral) {
			peripheral.onScanFinished(results);
		}
	}

	/**
	 * @return must be an instance of {@link RadarPeripheral} or its subclass
	 */
	protected abstract Object createPeripheral();

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
	}

	@Override
	public void serverTick() {
		super.serverTick();

		if (this.radarSignatureStrength > 0) {
			this.radarSignatureStrength /= 2;
		}

		if (!this.isEnabled()) {
			return;
		}

		if (this.scanRemaning <= 0) {
			if (this.autoScan) {
				this.queueScan();
			}
			if (!this.queuingScan) {
				return;
			}
			final int energyComsumption = this.getEnergyConsumption();
			if (energyComsumption < 0) {
				return;
			}
			final int energy = this.getEnergyStored();
			final int newEnergy = energy - energyComsumption;
			if (newEnergy < 0) {
				return;
			}
			if (!this.prepareScan()) {
				return;
			}
			this.setEnergyStored(newEnergy);
			this.scanRemaning = Math.min(1, this.getScanTime());
			this.queuingScan = false;
		} else {
			this.scanRemaning--;
			if (this.scanRemaning <= 0) {
				final List<ScanResult> results = new ArrayList<>();
				this.finalizeScan(results);
				this.onScanFinished(results);
			}
		}
	}

	public class ScanResult {
		private final double distance;
		private final double xRot, yRot;

		public ScanResult(final double distance, final double xRot, final double yRot) {
			this.distance = distance;
			this.xRot = xRot;
			this.yRot = yRot;
		}

		public final double getDistance() {
			return this.distance;
		}

		public final double getXRot() {
			return this.xRot;
		}

		public final double getYRot() {
			return this.yRot;
		}

		public void saveAsJSON(final Map<String, Object> data) {
			data.put("distance", this.distance);
			data.put("xRot", this.xRot);
			data.put("yRot", this.yRot);
		}

		public final Map<String, Object> toJSON() {
			final Map<String, Object> data = new HashMap<>();
			this.saveAsJSON(data);
			return data;
		}
	}
}
