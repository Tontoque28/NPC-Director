package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record DeleteNpcPayload(int npcEntityId) {

	public static final StreamCodec<RegistryFriendlyByteBuf, DeleteNpcPayload> STREAM_CODEC = StreamCodec.ofMember(DeleteNpcPayload::encode, DeleteNpcPayload::decode);

	public static void encode(DeleteNpcPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
	}

	public static DeleteNpcPayload decode(FriendlyByteBuf buf) {
		return new DeleteNpcPayload(buf.readVarInt());
	}
}
