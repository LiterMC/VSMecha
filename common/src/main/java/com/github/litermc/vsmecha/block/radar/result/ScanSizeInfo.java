package com.github.litermc.vsmecha.block.radar.result;

import java.util.Map;

public final class ScanSizeInfo implements ScanAdditonInfo {
	private final double width;
	private final double height;

	public ScanSizeInfo(final double width, final double height) {
		this.width = width;
		this.height = height;
	}

	public double getWidth() {
		return this.width;
	}

	public double getHeight() {
		return this.height;
	}

	@Override
	public String getId() {
		return "size";
	}

	@Override
	public void saveAsJSON(final Map<String, Object> data) {
		data.put("width", this.width);
		data.put("height", this.height);
	}
}
