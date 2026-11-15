package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public record SkinSyncPayload(String hash, byte[] png) {

	public static final int MAX_PNG = 16384;

	public static void encode(SkinSyncPayload payload, FriendlyByteBuf buf) {
		buf.writeUtf(payload.hash(), 32);
		buf.writeByteArray(payload.png());
	}

	public static SkinSyncPayload decode(FriendlyByteBuf buf) {
		String hash = buf.readUtf(32);
		return new SkinSyncPayload(hash, buf.readByteArray(MAX_PNG));
	}
}
