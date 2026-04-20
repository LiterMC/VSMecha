package com.github.litermc.vsmecha.block;

import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.world.PhysLevel;

public interface IPhysTickableBlockEntity {
	void physicsTick(PhysShip ship, PhysLevel world);
}
