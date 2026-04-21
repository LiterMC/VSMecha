package com.github.litermc.vsmecha.compat.computercraft.radar;

import com.github.litermc.vsmecha.block.radar.RadarBlockEntity;
import com.github.litermc.vsmecha.block.radar.result.ScanResult;
import com.github.litermc.vsmecha.compat.computercraft.EnergyBasedPeripheral;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class RadarPeripheral<T extends RadarBlockEntity> extends EnergyBasedPeripheral<T> {
	private final AttachedComputerSet computers = new AttachedComputerSet();

	protected RadarPeripheral(final T be) {
		super(be);
	}

	@Override
	public Set<String> getAdditionalTypes() {
		final Set<String> types = new HashSet<>(super.getAdditionalTypes());
		types.add("radar");
		return types;
	}

	@Override
	public void attach(final IComputerAccess computer) {
		this.computers.add(computer);
	}

	@Override
	public void detach(final IComputerAccess computer) {
		this.computers.remove(computer);
	}

	@LuaFunction
	public final int getMaxRadius() {
		return this.be.getMaxScanRadius();
	}

	@LuaFunction
	public final int getRadius() {
		return this.be.getScanRadius();
	}

	@LuaFunction
	public final void setRadius(final int radius) {
		this.be.setScanRadius(radius);
	}

	@LuaFunction
	public final int getEnergyConsumption() {
		return this.be.getEnergyConsumption();
	}

	@LuaFunction
	public final int getScanTime() {
		return this.be.getScanTime();
	}

	@LuaFunction
	public final boolean getAutoScan() {
		return this.be.getAutoScan();
	}

	@LuaFunction
	public final void setAutoScan(final boolean autoScan) {
		this.be.setAutoScan(autoScan);
	}

	@LuaFunction
	public final boolean isScanning() {
		return this.be.isScanning();
	}

	@LuaFunction
	public final void queueScan() {
		this.be.queueScan();
	}

	public void onScanFinished(final List<ScanResult> results) {
		if (!this.computers.hasComputers()) {
			return;
		}
		final List<Map<String, Object>> encodedResults = results.stream().map(ScanResult::toJSON).toList();
		this.computers.forEach((computer) -> computer.queueEvent("radar_scan", computer.getAttachmentName(), encodedResults));
	}
}
