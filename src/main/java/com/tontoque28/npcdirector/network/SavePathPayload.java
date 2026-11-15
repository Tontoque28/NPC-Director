package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record SavePathPayload(int npcEntityId, String name, PathData path) {

	public static final StreamCodec<RegistryFriendlyByteBuf, SavePathPayload> STREAM_CODEC = StreamCodec.ofMember(SavePathPayload::encode, SavePathPayload::decode);

	public static void encode(SavePathPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.name(), 64);
		payload.path().write(buf);
	}

	public static SavePathPayload decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		String name = buf.readUtf(64);
		return new SavePathPayload(id, name, PathData.read(buf));
	}
}
