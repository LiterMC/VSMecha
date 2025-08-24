package com.github.litermc.vsmecha.platform;

public interface EnergyInterface {
	int pushEnergy(int available, boolean simulate);
	int pullEnergy(int needs, boolean simulate);
}
