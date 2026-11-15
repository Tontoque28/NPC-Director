package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public final class OutputSubscribePayload {
	private final boolean on;
	public OutputSubscribePayload(boolean on) {
		this.on = on;
	}
	public boolean on() {
		return on;
	}


	public static void encode(OutputSubscribePayload payload, FriendlyByteBuf buf) {
		buf.writeBoolean(payload.on());
	}

	public static OutputSubscribePayload decode(FriendlyByteBuf buf) {
		return new OutputSubscribePayload(buf.readBoolean());
	}
}
