package com.tontoque28.npcdirector.network;

import net.minecraft.network.PacketBuffer;

public final class SaveDialoguePayload {
	private final int npcEntityId;
	private final String name;
	private final DialogueData dialogue;
	public SaveDialoguePayload(int npcEntityId, String name, DialogueData dialogue) {
		this.npcEntityId = npcEntityId;
		this.name = name;
		this.dialogue = dialogue;
	}
	public int npcEntityId() {
		return npcEntityId;
	}
	public String name() {
		return name;
	}
	public DialogueData dialogue() {
		return dialogue;
	}


	public static void encode(SaveDialoguePayload payload, PacketBuffer buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.name(), 64);
		payload.dialogue().write(buf);
	}

	public static SaveDialoguePayload decode(PacketBuffer buf) {
		int id = buf.readVarInt();
		String name = buf.readUtf(64);
		return new SaveDialoguePayload(id, name, DialogueData.read(buf));
	}
}
