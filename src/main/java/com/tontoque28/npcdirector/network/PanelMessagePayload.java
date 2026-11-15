package com.tontoque28.npcdirector.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;

public final class PanelMessagePayload {
	private final ITextComponent message;

	public PanelMessagePayload(ITextComponent message) {
		this.message = message;
	}

	public ITextComponent message() {
		return message;
	}

	public static void encode(PanelMessagePayload payload, PacketBuffer buf) {
		buf.writeComponent(payload.message());
	}

	public static PanelMessagePayload decode(PacketBuffer buf) {
		return new PanelMessagePayload(buf.readComponent());
	}
}
