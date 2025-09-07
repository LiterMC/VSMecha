package com.github.litermc.vsmecha.block.radar.result;

import java.util.Map;

public final class ScanTypeInfo implements ScanAdditonInfo {
	public static final ScanTypeInfo ENTITY = new ScanTypeInfo("entity");
	public static final ScanTypeInfo SHIP = new ScanTypeInfo("ship");

	private final String type;

	public ScanTypeInfo(final String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}

	@Override
	public String getId() {
		return "type";
	}

	@Override
	public void saveAsJSON(final Map<String, Object> data) {
		data.put("type", this.type);
	}
}
