package com.github.litermc.vsmecha.compat;

import com.github.litermc.vsmecha.platform.PlatformHelper;

public enum CompatMods {
	COMPUTERCRAFT("computercraft"),
	CREATE("create"),
	JADE("jade");

	private final String modId;

	private CompatMods(final String modId) {
		this.modId = modId;
	}

	public String getId() {
		return this.modId;
	}

	public boolean isLoaded() {
		return PlatformHelper.get().isModLoaded(this.getId());
	}
}
