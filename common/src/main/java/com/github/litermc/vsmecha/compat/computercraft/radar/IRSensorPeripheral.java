package com.github.litermc.vsmecha.compat.computercraft.radar;

import com.github.litermc.vsmecha.block.radar.IRSensorBlockEntity;

public class IRSensorPeripheral extends RadarPeripheral<IRSensorBlockEntity> {
	public IRSensorPeripheral(final IRSensorBlockEntity be) {
		super(be);
	}

	@Override
	public String getType() {
		return "ir_sensor";
	}
}
