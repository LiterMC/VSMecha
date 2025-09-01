package com.github.litermc.vsmecha.block.radar;

import com.github.litermc.vsmecha.block.energy.EnergyBasedBlockEntity;
import com.github.litermc.vsmecha.compat.CompatMods;
import com.github.litermc.vsmecha.compat.computercraft.radar.RadarPeripheral;

import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RadarBlockEntity extends EnergyBasedBlockEntity {
	private final FrontAndTop orientation;
	private volatile int radius;
	private volatile int scanRemaning = 0;
	private volatile boolean autoScan = false;
	private volatile boolean queuingScan = false;

	private int radarSignatureStrength = 0;

	protected RadarBlockEntity(final BlockEntityType<? extends RadarBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		this.orientation = state.getValue(RadarBlock.ORIENTATION);
		this.radius = this.getMaxScanRadius();
	}

	public FrontAndTop getOrientation() {
		return this.orientation;
	}

	public abstract int getMaxScanRadius();

	public int getScanRadius() {
		return this.radius;
	}

	public void setScanRadius(int radius) {
		radius = Math.min(Math.max(radius, 0), this.getMaxScanRadius());
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

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.radius = data.getInt("ScanRadius");
		this.scanRemaning = data.getInt("ScanRemaning");
		this.autoScan = data.getBoolean("AutoScan");
		this.queuingScan = data.getBoolean("QueuedScan");
		this.radarSignatureStrength = data.getInt("RadarSignature");
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putInt("ScanRadius", this.radius);
		data.putInt("ScanRemaning", this.scanRemaning);
		data.putBoolean("AutoScan", this.autoScan);
		data.putBoolean("QueuedScan", this.queuingScan);
		data.putInt("RadarSignature", this.radarSignatureStrength);
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

	/*** begin utility methods ***/

	public static double mapValue(
		final double value,
		final double oldMin, final double oldMax,
		final double newMin, final double newMax
	) {
		return (value - oldMin) / (oldMax - oldMin) * (newMax - newMin) + newMin;
	}

	public static double generateRandom(final Object hashSource, final double min, final double max) {
		return (((double) (hashSource.hashCode())) / Integer.MAX_VALUE + 1) / 2 * (max - min) + min;
	}

	public static double generateRandom(final int hashCode, final double min, final double max) {
		return (((double) (hashCode)) / Integer.MAX_VALUE + 1) / 2 * (max - min) + min;
	}

	public static double applyError(final double value, final double error) {
		final double e = value * error;
		return generateRandom(value, value - e, value + e);
	}

	public static double closestDist(final AABB box, final Vec3 pos) {
		final double x = pos.x;
		final double y = pos.y;
		final double z = pos.z;
		double dist = 0;
		if (x < box.minX) {
			final double d = box.minX - x;
			dist += d * d;
		} else if (x > box.maxX) {
			final double d = x - box.maxX;
			dist += d * d;
		}
		if (y < box.minY) {
			final double d = box.minY - y;
			dist += d * d;
		} else if (y > box.maxY) {
			final double d = y - box.maxY;
			dist += d * d;
		}
		if (z < box.minZ) {
			final double d = box.minZ - z;
			dist += d * d;
		} else if (z > box.maxZ) {
			final double d = z - box.maxZ;
			dist += d * d;
		}
		return Math.sqrt(dist);
	}

	/*** end utility methods ***/

	public static class ScanResult {
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

	public static class ScanResultWithType extends ScanResult {
		public static final String TYPE_ENTITY = "entity";
		public static final String TYPE_SHIP = "ship";

		private final String type;

		public ScanResultWithType(
			final double distance,
			final double xRot,
			final double yRot,
			final String type
		) {
			super(distance, xRot, yRot);
			this.type = type;
		}

		public final String getType() {
			return this.type;
		}

		public void saveAsJSON(final Map<String, Object> data) {
			super.saveAsJSON(data);
			data.put("type", this.type);
		}
	}
}
