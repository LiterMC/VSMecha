package com.github.litermc.vsmecha.accessor;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import java.util.List;

public interface ContraptionHolder {
	List<AbstractContraptionEntity> vsmecha$clearContraptions();

	void vsmecha$restoreContraptions(List<AbstractContraptionEntity> contraptions);
}
