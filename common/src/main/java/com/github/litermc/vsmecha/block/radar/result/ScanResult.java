package com.github.litermc.vsmecha.block.radar.result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ScanResult {
	private final double distance;
	private final double xRot, yRot;

	private final HashMap<String, ScanAdditonInfo> additions = new HashMap<>();

	public ScanResult(final double distance, final double xRot, final double yRot) {
		this.distance = distance;
		this.xRot = xRot;
		this.yRot = yRot;
	}

	public double getDistance() {
		return this.distance;
	}

	public double getXRot() {
		return this.xRot;
	}

	public double getYRot() {
		return this.yRot;
	}

	public List<ScanAdditonInfo> getAdditionInfos() {
		return List.copyOf(this.additions.values());
	}

	public ScanAdditonInfo getAdditionInfo(final String id) {
		return this.additions.get(id);
	}

	public ScanResult appendInfo(final ScanAdditonInfo info) {
		this.additions.put(info.getId(), info);
		return this;
	}

	public void saveAsJSON(final Map<String, Object> data) {
		data.put("distance", this.distance);
		data.put("xRot", this.xRot);
		data.put("yRot", this.yRot);
		for (final ScanAdditonInfo info : this.additions.values()) {
			info.saveAsJSON(data);
		}
	}

	public Map<String, Object> toJSON() {
		final Map<String, Object> data = new HashMap<>();
		this.saveAsJSON(data);
		return data;
	}
}
