package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public final class SkinSyncPayload {
	public static final int MAX_PNG = 16384;

	private final String hash;
	private final byte[] png;

	public SkinSyncPayload(String hash, byte[] png) {
		this.hash = hash;
		this.png = png;
	}

	public String hash() {
		return hash;
	}

	public byte[] png() {
		return png;
	}

	public static void encode(SkinSyncPayload payload, FriendlyByteBuf buf) {
		buf.writeUtf(payload.hash(), 32);
		buf.writeByteArray(payload.png());
	}

	public static SkinSyncPayload decode(FriendlyByteBuf buf) {
		String hash = buf.readUtf(32);
		return new SkinSyncPayload(hash, buf.readByteArray(MAX_PNG));
	}
}
