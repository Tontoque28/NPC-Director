package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public record PanelMessagePayload(Component message) {

	public static void encode(PanelMessagePayload payload, FriendlyByteBuf buf) {
		buf.writeComponent(payload.message());
	}

	public static PanelMessagePayload decode(FriendlyByteBuf buf) {
		return new PanelMessagePayload(buf.readComponent());
	}
}
