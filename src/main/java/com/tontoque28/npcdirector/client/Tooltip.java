package com.tontoque28.npcdirector.client;

import net.minecraft.util.text.ITextComponent;

public final class Tooltip {

	private final ITextComponent text;

	private Tooltip(ITextComponent text) {
		this.text = text;
	}

	public static Tooltip create(ITextComponent text) {
		return new Tooltip(text);
	}

	public ITextComponent text() {
		return text;
	}
}
