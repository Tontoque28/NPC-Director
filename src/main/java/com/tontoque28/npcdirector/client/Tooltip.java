package com.tontoque28.npcdirector.client;

import net.minecraft.network.chat.Component;

public final class Tooltip {

	private final Component text;

	private Tooltip(Component text) {
		this.text = text;
	}

	public static Tooltip create(Component text) {
		return new Tooltip(text);
	}

	public Component text() {
		return text;
	}
}
