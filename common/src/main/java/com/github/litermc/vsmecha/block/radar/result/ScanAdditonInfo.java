package com.github.litermc.vsmecha.block.radar.result;

import java.util.Map;

public interface ScanAdditonInfo {
	/**
	 * @return unique id for this scan info
	 */
	String getId();

	void saveAsJSON(Map<String, Object> data);
}
