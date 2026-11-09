package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public final class DeleteNpcPayload {

	private final int npcEntityId;

	public DeleteNpcPayload(int npcEntityId) {
		this.npcEntityId = npcEntityId;
	}

	public int npcEntityId() {
		return npcEntityId;
	}

	public static void encode(DeleteNpcPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
	}

	public static DeleteNpcPayload decode(FriendlyByteBuf buf) {
		return new DeleteNpcPayload(buf.readVarInt());
	}
}
