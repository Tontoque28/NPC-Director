package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public record DeleteNpcPayload(int npcEntityId) {

	public static void encode(DeleteNpcPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
	}

	public static DeleteNpcPayload decode(FriendlyByteBuf buf) {
		return new DeleteNpcPayload(buf.readVarInt());
	}
}
