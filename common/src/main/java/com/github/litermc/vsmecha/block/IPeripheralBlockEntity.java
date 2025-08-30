package com.github.litermc.vsmecha.block;

import com.github.litermc.vsmecha.compat.computercraft.network.ShipModemPeripheral;

public interface IPeripheralBlockEntity {
	Object getOrCreatePeripheral();

	/**
	 * @return {@code null} or an instance of {@link ShipModemPeripheral}
	 */
	Object getShipModemPeripheral();
}
