package com.github.litermc.vsmecha.block.energy;

import com.github.litermc.vsmecha.enet.EnergyNetwork;

import java.util.function.IntUnaryOperator;

public interface IEnergyBlockEntity {
	/**
	 * Invoke when energy network is changed.
	 * This will never be invoked if the block is not placed on a ship.
	 *
	 * @param network new energy network, or {@code null} if no network is available
	 */
	void onEnergyNetworkChanged(EnergyNetwork network);

	/**
	 * Highest priority means energy will be provided first and drained last.
	 * Lowest priority means energy will be provided last and drained first.
	 *
	 * @return energy priority
	 * @see setEnergyPriority
	 */
	int getEnergyPriority();

	/**
	 * Update the block's energy priority.
	 * The block should inform {@link EnergyNetwork} its priority is changed via {@link EnergyNetwork#updatePriority}.
	 * Priority should not be changed during network ticks.
	 *
	 * @param priority changed priority
	 * @see getEnergyPriority
	 */
	void setEnergyPriority(int priority);

	/**
	 * Invoked before {@link tickEnergyInput}, in ascending order of priority.
	 * The result should never becomes lower before the network tick is finished, except {@link consumeEnergy} is invoked.
	 * When overriding this method, {@link consumeEnergy} should also be implemented.
	 *
	 * @return available energy in the energy source
	 */
	default int tickEnergySource() {
		return 0;
	}

	/**
	 * Invoked when energy is drained from this block.
	 * {@link tickEnergySource}'s result after this invoke should never be less than it's previous result minus {@code amount}.
	 * This method will only be invoked during network ticks.
	 *
	 * @param amount energy needs, always greater than zero and less than {@link tickEnergySource}'s result
	 */
	default void consumeEnergy(final int amount) {
		throw new UnsupportedOperationException("consumeEnergy must be implemented along with tickEnergySource");
	}

	/**
	 * Invoked after {@link tickEnergySource}, in descending order of priority
	 *
	 * @param available available unused energy in the network, will always be greater than zero
	 * @return used energy, should never less than zero or greater than {@code available}
	 */
	default int tickEnergyInput(final int available) {
		return 0;
	}
}
