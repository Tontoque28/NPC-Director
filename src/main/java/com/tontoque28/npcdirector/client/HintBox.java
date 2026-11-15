package com.tontoque28.npcdirector.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class HintBox extends EditBox implements Tipped {

	private final Font fontRef;
	private Component hint;
	private Component tip;

	public HintBox(Font font, int x, int y, int width, int height, Component message) {
		super(font, x, y, width, height, message);
		this.fontRef = font;
	}

	public void setHint(Component hint) {
		this.hint = hint;
	}

	public void setTooltip(Tooltip tooltip) {
		this.tip = tooltip.text();
	}

	@Override
	public Component tip() {
		return tip;
	}

	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		super.renderButton(poseStack, mouseX, mouseY, partialTick);
		if (hint != null && this.getValue().isEmpty() && !this.isFocused() && this.visible) {
			String shown = fontRef.plainSubstrByWidth(hint.getString(), this.getWidth() - 10);
			fontRef.draw(poseStack, shown, (float) (this.x + 4), (float) (this.y + (this.height - 8) / 2), 0xFF808080);
		}
	}
}
