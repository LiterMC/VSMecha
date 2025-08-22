package com.github.litermc.vsmecha.block;

import com.github.litermc.vsmecha.enet.EnergyNetwork;

import java.util.function.IntUnaryOperator;

public interface IMechaBlockEntity {
	/**
	 * Invoke when energy network is changed.
	 * This will never be invoked if the block is not placed on a ship.
	 *
	 * @param network new energy network
	 */
	void setEnergyNetwork(EnergyNetwork network);

	/**
	 * As an energy source block: higher priority means drain last.
	 * As an energy consumer block: higher priority means provide first.
	 *
	 * @return energy priority
	 */
	default int getEnergyPriority() {
		return 1000;
	}

	/**
	 * Invoked before {@link tickEnergyUse}, in ascending order of priority
	 *
	 * @return available energy in the energy source
	 */
	default int tickEnergySource() {
		return 0;
	}

	/**
	 * Invoked after {@link tickEnergySource}, in descending order of priority
	 *
	 * @param available available unused energy in the network
	 * @return remaining unused energy
	 */
	default int tickEnergyUse(final int available) {
		return available;
	}
}
