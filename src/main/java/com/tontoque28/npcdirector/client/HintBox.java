package com.tontoque28.npcdirector.client;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;

public class HintBox extends TextFieldWidget implements Tipped {

	private final FontRenderer fontRef;
	private ITextComponent hint;
	private ITextComponent tip;

	public HintBox(FontRenderer font, int x, int y, int width, int height, ITextComponent message) {
		super(font, x, y, width, height, message);
		this.fontRef = font;
	}

	public void setHint(ITextComponent hint) {
		this.hint = hint;
	}

	public void setTooltip(Tooltip tooltip) {
		this.tip = tooltip.text();
	}

	@Override
	public ITextComponent tip() {
		return tip;
	}

	@Override
	public void renderButton(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
		super.renderButton(poseStack, mouseX, mouseY, partialTick);
		if (hint != null && this.getValue().isEmpty() && !this.isFocused() && this.visible) {
			String shown = fontRef.plainSubstrByWidth(hint.getString(), this.getWidth() - 10);
			fontRef.draw(poseStack, shown, (float) (this.x + 4), (float) (this.y + (this.height - 8) / 2), 0xFF808080);
		}
	}
}
