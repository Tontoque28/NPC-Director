package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public final class ImportSkinPayload {
	public static final int MAX_CHUNK = 4096;

	private final int npcEntityId;
	private final int uploadId;
	private final int index;
	private final int total;
	private final int totalBytes;
	private final byte[] data;

	public ImportSkinPayload(int npcEntityId, int uploadId, int index, int total, int totalBytes, byte[] data) {
		this.npcEntityId = npcEntityId;
		this.uploadId = uploadId;
		this.index = index;
		this.total = total;
		this.totalBytes = totalBytes;
		this.data = data;
	}

	public int npcEntityId() {
		return npcEntityId;
	}

	public int uploadId() {
		return uploadId;
	}

	public int index() {
		return index;
	}

	public int total() {
		return total;
	}

	public int totalBytes() {
		return totalBytes;
	}

	public byte[] data() {
		return data;
	}

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
