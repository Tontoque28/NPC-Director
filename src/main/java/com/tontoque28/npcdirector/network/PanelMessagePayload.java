package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public final class PanelMessagePayload {
	private final Component message;

	public PanelMessagePayload(Component message) {
		this.message = message;
	}

	public Component message() {
		return message;
	}

	public static void encode(PanelMessagePayload payload, FriendlyByteBuf buf) {
		buf.writeComponent(payload.message());
	}

	public static PanelMessagePayload decode(FriendlyByteBuf buf) {
		return new PanelMessagePayload(buf.readComponent());
	}
}
