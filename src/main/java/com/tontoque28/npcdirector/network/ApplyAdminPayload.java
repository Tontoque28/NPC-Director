package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ApplyAdminPayload(int npcEntityId, String name, int relevance, boolean targetable, String pathId, String dialogueId) {

	public static final StreamCodec<RegistryFriendlyByteBuf, ApplyAdminPayload> STREAM_CODEC = StreamCodec.ofMember(ApplyAdminPayload::encode, ApplyAdminPayload::decode);

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
