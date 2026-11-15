package com.tontoque28.npcdirector.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class Btn {

	public static final class Tip extends Button implements Tipped {

		private Component tip;

		Tip(int x, int y, int w, int h, Component message, Button.OnPress onPress, Component tip) {
			super(x, y, w, h, message, onPress);
			this.tip = tip;
		}

		@Override
		public Component tip() {
			return tip;
		}
	}

	public static final class Builder {

		private final Component message;
		private final Button.OnPress press;
		private int x;
		private int y;
		private int w = 150;
		private int h = 20;
		private Component tip;

		private Builder(Component message, Button.OnPress press) {
			this.message = message;
			this.press = press;
		}

		public Builder bounds(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			return this;
		}

		public Builder tooltip(Tooltip tooltip) {
			this.tip = tooltip.text();
			return this;
		}

		public Button build() {
			return new Tip(x, y, w, h, message, press, tip);
		}
	}

	private Btn() {
	}

	public static Builder builder(Component message, Button.OnPress press) {
		return new Builder(message, press);
	}
}
