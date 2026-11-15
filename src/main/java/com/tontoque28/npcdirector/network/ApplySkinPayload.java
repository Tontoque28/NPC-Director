package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ApplySkinPayload(int npcEntityId, String skinName) {

	public static final StreamCodec<RegistryFriendlyByteBuf, ApplySkinPayload> STREAM_CODEC = StreamCodec.ofMember(ApplySkinPayload::encode, ApplySkinPayload::decode);

	public static final int NAME_MAX = 32;

	public static void encode(ApplySkinPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.skinName(), NAME_MAX);
	}

	public static ApplySkinPayload decode(FriendlyByteBuf buf) {
		return new ApplySkinPayload(buf.readVarInt(), buf.readUtf(NAME_MAX));
	}
}
