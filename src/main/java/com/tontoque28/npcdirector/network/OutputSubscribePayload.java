package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record OutputSubscribePayload(boolean on) {

	public static final StreamCodec<RegistryFriendlyByteBuf, OutputSubscribePayload> STREAM_CODEC = StreamCodec.ofMember(OutputSubscribePayload::encode, OutputSubscribePayload::decode);

	public static void encode(OutputSubscribePayload payload, FriendlyByteBuf buf) {
		buf.writeBoolean(payload.on());
	}

	public static OutputSubscribePayload decode(FriendlyByteBuf buf) {
		return new OutputSubscribePayload(buf.readBoolean());
	}
}
