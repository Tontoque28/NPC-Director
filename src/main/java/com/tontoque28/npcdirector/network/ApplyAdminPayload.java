package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public final class ApplyAdminPayload {

	private final int npcEntityId;
	private final String name;
	private final int relevance;
	private final boolean targetable;
	private final String pathId;
	private final String dialogueId;

	public ApplyAdminPayload(int npcEntityId, String name, int relevance, boolean targetable, String pathId, String dialogueId) {
		this.npcEntityId = npcEntityId;
		this.name = name;
		this.relevance = relevance;
		this.targetable = targetable;
		this.pathId = pathId;
		this.dialogueId = dialogueId;
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

	public static void encode(ApplyAdminPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.name(), 64);
		buf.writeVarInt(payload.relevance());
		buf.writeBoolean(payload.targetable());
		buf.writeUtf(payload.pathId(), 128);
		buf.writeUtf(payload.dialogueId(), 128);
	}

	public static ApplyAdminPayload decode(FriendlyByteBuf buf) {
		return new ApplyAdminPayload(buf.readVarInt(), buf.readUtf(64), buf.readVarInt(), buf.readBoolean(), buf.readUtf(128), buf.readUtf(128));
	}
}
