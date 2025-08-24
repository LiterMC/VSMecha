package com.github.litermc.vsmecha.block.energy;

public interface IThermalBlockEntity {
	int getHeat();

	void transferHeat(int heat);
}
