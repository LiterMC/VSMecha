package com.github.litermc.vsmecha.block;

import org.valkyrienskies.core.api.ships.PhysShip;

import java.util.function.Function;

public interface IPhysTickableBlockEntity {
	void physicsTick(PhysShip ship, Function<Long, PhysShip> lookup);
}
