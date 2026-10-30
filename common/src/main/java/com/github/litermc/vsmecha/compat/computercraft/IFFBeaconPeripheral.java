package com.github.litermc.vsmecha.compat.computercraft;

import com.github.litermc.vsmecha.block.radar.IFFBeaconBlockEntity;

public class IFFBeaconPeripheral extends EnergyBasedPeripheral<IFFBeaconBlockEntity> {
	public IFFBeaconPeripheral(IFFBeaconBlockEntity be) {
		super(be);
	}

	@Override
	public String getType() {
		return "iff_beacon";
	}

}
