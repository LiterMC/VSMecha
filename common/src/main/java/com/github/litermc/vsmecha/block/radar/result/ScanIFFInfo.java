package com.github.litermc.vsmecha.block.radar.result;

import java.util.Map;
import java.util.UUID;

public final class ScanIFFInfo implements ScanAdditonInfo {
	private final long shipId;
	private final UUID owner;
	private final boolean isAlly;
	private final boolean markedHostile;

	public ScanIFFInfo(final long shipId, final UUID owner, final boolean isAlly, final boolean markedHostile) {
		this.shipId = shipId;
		this.owner = owner;
		this.isAlly = isAlly;
		this.markedHostile = markedHostile;
	}

	public long getShipId() {
		return this.shipId;
	}

	public UUID getOwner() {
		return this.owner;
	}

	public boolean isAlly() {
		return this.isAlly;
	}

	public boolean markedHostile() {
		return this.markedHostile;
	}

	@Override
	public String getId() {
		return "iff";
	}

	@Override
	public void saveAsJSON(final Map<String, Object> data) {
		data.put("shipId", this.shipId);
		data.put("owner", this.owner == null ? null : this.owner.toString());
		data.put("isAlly", this.isAlly);
		data.put("markedHostile", this.markedHostile);
	}
}
