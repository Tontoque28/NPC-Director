package com.tontoque28.npcdirector.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class HoverButton extends Button.Plain {

	public static final int HOVER_TINT = 0x7055B8FF;
	public static final int SELECTED_TINT = 0x7055FF55;

	public boolean selected;
	public int tint;

	protected HoverButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
	}

	public static Builder hoverBuilder(Component message, Button.OnPress onPress) {
		return new Builder(message, onPress);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractContents(g, mouseX, mouseY, partialTick);
		int x = this.getX();
		int y = this.getY();
		int x1 = x + this.getWidth();
		int y1 = y + this.getHeight();
		if (tint != 0) {
			g.fill(x + 1, y + 1, x1 - 1, y1 - 1, tint);
		}
		if (selected) {
			g.fill(x + 1, y + 1, x1 - 1, y1 - 1, SELECTED_TINT);
			g.outline(x, y, this.getWidth(), this.getHeight(), 0xFF55FF55);
		} else if (this.active && this.isHoveredOrFocused()) {
			g.fill(x + 1, y + 1, x1 - 1, y1 - 1, HOVER_TINT);
			g.outline(x, y, this.getWidth(), this.getHeight(), 0xFFFFFFFF);
		}
	}

	public static class Builder {

		private final Component message;
		private final Button.OnPress onPress;
		private Tooltip tooltip;
		private int x;
		private int y;
		private int width = 150;
		private int height = 20;

		Builder(Component message, Button.OnPress onPress) {
			this.message = message;
			this.onPress = onPress;
		}

		public Builder pos(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}

		public Builder width(int width) {
			this.width = width;
			return this;
		}

		public Builder size(int width, int height) {
			this.width = width;
			this.height = height;
			return this;
		}

		public Builder bounds(int x, int y, int width, int height) {
			return pos(x, y).size(width, height);
		}

		public Builder tooltip(Tooltip tooltip) {
			this.tooltip = tooltip;
			return this;
		}

		public HoverButton build() {
			HoverButton b = new HoverButton(x, y, width, height, message, onPress);
			b.setTooltip(tooltip);
			return b;
		}
	}
}
