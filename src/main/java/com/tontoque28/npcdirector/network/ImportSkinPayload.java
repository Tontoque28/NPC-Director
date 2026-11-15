package com.tontoque28.npcdirector.network;

import com.tontoque28.npcdirector.NpcDirector;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ImportSkinPayload(int npcEntityId, int uploadId, int index, int total, int totalBytes, byte[] data) implements CustomPacketPayload {

	public static final Type<ImportSkinPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "import_skin"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ImportSkinPayload> STREAM_CODEC = StreamCodec.ofMember(ImportSkinPayload::encode, ImportSkinPayload::decode);

	public static final int MAX_CHUNK = 4096;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
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
