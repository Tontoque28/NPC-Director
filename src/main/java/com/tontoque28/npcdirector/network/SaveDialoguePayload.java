package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public record SaveDialoguePayload(int npcEntityId, String name, DialogueData dialogue) {

	public static void encode(SaveDialoguePayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.name(), 64);
		payload.dialogue().write(buf);
	}

	public static SaveDialoguePayload decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		String name = buf.readUtf(64);
		return new SaveDialoguePayload(id, name, DialogueData.read(buf));
	}
}
