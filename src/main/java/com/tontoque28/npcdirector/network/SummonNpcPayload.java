package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public final class SummonNpcPayload {

	public static void encode(SummonNpcPayload payload, FriendlyByteBuf buf) {
	}

	public static SummonNpcPayload decode(FriendlyByteBuf buf) {
		return new SummonNpcPayload();
	}
}
