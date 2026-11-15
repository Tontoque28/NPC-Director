package com.tontoque28.npcdirector.client;

import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;

public final class Btn {

	public static final class Tip extends Button implements Tipped {

		private ITextComponent tip;

		Tip(int x, int y, int w, int h, ITextComponent message, Button.IPressable onPress, ITextComponent tip) {
			super(x, y, w, h, message, onPress);
			this.tip = tip;
		}

		@Override
		public ITextComponent tip() {
			return tip;
		}
	}

	public static final class Builder {

		private final ITextComponent message;
		private final Button.IPressable press;
		private int x;
		private int y;
		private int w = 150;
		private int h = 20;
		private ITextComponent tip;

		private Builder(ITextComponent message, Button.IPressable press) {
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

	public static Builder builder(ITextComponent message, Button.IPressable press) {
		return new Builder(message, press);
	}
}
