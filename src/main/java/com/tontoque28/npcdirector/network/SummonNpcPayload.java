package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record SummonNpcPayload() {

	public static final StreamCodec<RegistryFriendlyByteBuf, SummonNpcPayload> STREAM_CODEC = StreamCodec.ofMember(SummonNpcPayload::encode, SummonNpcPayload::decode);

	public static void encode(SummonNpcPayload payload, FriendlyByteBuf buf) {
	}

	public static SummonNpcPayload decode(FriendlyByteBuf buf) {
		return new SummonNpcPayload();
	}
}
