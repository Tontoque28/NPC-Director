package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public record ApplySkinPayload(int npcEntityId, String skinName) {

	public static final int NAME_MAX = 32;

	public static void encode(ApplySkinPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.skinName(), NAME_MAX);
	}

	public static ApplySkinPayload decode(FriendlyByteBuf buf) {
		return new ApplySkinPayload(buf.readVarInt(), buf.readUtf(NAME_MAX));
	}
}
