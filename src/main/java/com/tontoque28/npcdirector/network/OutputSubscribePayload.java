package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public record OutputSubscribePayload(boolean on) {

	public static void encode(OutputSubscribePayload payload, FriendlyByteBuf buf) {
		buf.writeBoolean(payload.on());
	}

	public static OutputSubscribePayload decode(FriendlyByteBuf buf) {
		return new OutputSubscribePayload(buf.readBoolean());
	}
}
