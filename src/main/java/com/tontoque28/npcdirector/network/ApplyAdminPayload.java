package com.tontoque28.npcdirector.network;

import net.minecraft.network.PacketBuffer;

public final class ApplyAdminPayload {
	private final int npcEntityId;
	private final String name;
	private final int relevance;
	private final boolean targetable;
	private final String pathId;
	private final String dialogueId;
	private final AdminFields fields;
	public ApplyAdminPayload(int npcEntityId, String name, int relevance, boolean targetable, String pathId, String dialogueId, AdminFields fields) {
		this.npcEntityId = npcEntityId;
		this.name = name;
		this.relevance = relevance;
		this.targetable = targetable;
		this.pathId = pathId;
		this.dialogueId = dialogueId;
		this.fields = fields;
	}
	public int npcEntityId() {
		return npcEntityId;
	}
	public String name() {
		return name;
	}
	public int relevance() {
		return relevance;
	}
	public boolean targetable() {
		return targetable;
	}
	public String pathId() {
		return pathId;
	}
	public String dialogueId() {
		return dialogueId;
	}
	public AdminFields fields() {
		return fields;
	}


	public static void encode(ApplyAdminPayload payload, PacketBuffer buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.name(), 64);
		buf.writeVarInt(payload.relevance());
		buf.writeBoolean(payload.targetable());
		buf.writeUtf(payload.pathId(), 128);
		buf.writeUtf(payload.dialogueId(), 128);
		payload.fields().write(buf);
	}

	public static ApplyAdminPayload decode(PacketBuffer buf) {
		int id = buf.readVarInt();
		String name = buf.readUtf(64);
		int relevance = buf.readVarInt();
		boolean targetable = buf.readBoolean();
		String path = buf.readUtf(128);
		String dialogue = buf.readUtf(128);
		return new ApplyAdminPayload(id, name, relevance, targetable, path, dialogue, AdminFields.read(buf));
	}
}
