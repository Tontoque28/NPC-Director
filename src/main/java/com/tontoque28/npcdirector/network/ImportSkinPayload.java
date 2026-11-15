package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public record ImportSkinPayload(int npcEntityId, int uploadId, int index, int total, int totalBytes, byte[] data) {

	public static final int MAX_CHUNK = 4096;

	public static void encode(ImportSkinPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeVarInt(payload.uploadId());
		buf.writeVarInt(payload.index());
		buf.writeVarInt(payload.total());
		buf.writeVarInt(payload.totalBytes());
		buf.writeByteArray(payload.data());
	}

	public static ImportSkinPayload decode(FriendlyByteBuf buf) {
		int npc = buf.readVarInt();
		int upload = buf.readVarInt();
		int index = buf.readVarInt();
		int total = buf.readVarInt();
		int bytes = buf.readVarInt();
		return new ImportSkinPayload(npc, upload, index, total, bytes, buf.readByteArray(MAX_CHUNK));
	}
}
