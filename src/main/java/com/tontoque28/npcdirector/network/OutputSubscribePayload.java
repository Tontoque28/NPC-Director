package com.tontoque28.npcdirector.network;

import net.minecraft.network.PacketBuffer;

public final class OutputSubscribePayload {
	private final boolean on;
	public OutputSubscribePayload(boolean on) {
		this.on = on;
	}
	public boolean on() {
		return on;
	}


	public static void encode(OutputSubscribePayload payload, PacketBuffer buf) {
		buf.writeBoolean(payload.on());
	}

	public static OutputSubscribePayload decode(PacketBuffer buf) {
		return new OutputSubscribePayload(buf.readBoolean());
	}
}
