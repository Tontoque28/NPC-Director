package com.tontoque28.npcdirector.client;

import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tontoque28.npcdirector.network.ChooseDialoguePayload;
import com.tontoque28.npcdirector.network.CloseDialoguePayload;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;

public class DialogueScreen extends Screen {

	private static final int OPTION_HEIGHT = 22;
	private static final int LINE_HEIGHT = 10;

	private final String text;
	private final List<String> optionTexts;
	private List<FormattedCharSequence> lines = Collections.emptyList();
	private int textTop;
	private int textHeight;
	private int textScroll;
	private int optionPage;
	private int optionsPerPage = 1;
	private int optionsTop;
	private Button previous;
	private Button next;

	public DialogueScreen(String text, List<String> optionTexts) {
		super(new TextComponent(text));
		this.text = text;
		this.optionTexts = optionTexts;
	}

	@Override
	protected void init() {
		int boxWidth = Math.max(60, Math.min(this.width - 40, 320));
		this.lines = this.font.split(FormattedText.of(text), boxWidth);
		this.textTop = 20;
		int maxTextHeight = Math.max(LINE_HEIGHT * 2, (this.height - 40) / 2 - 20);
		this.textHeight = Math.min(lines.size() * LINE_HEIGHT, maxTextHeight);
		this.textScroll = Math.min(this.textScroll, maxTextScroll());
		this.optionsTop = textTop + textHeight + 14;
		int available = this.height - optionsTop - 30;
		this.optionsPerPage = Math.max(1, available / OPTION_HEIGHT);
		if (optionTexts.size() > optionsPerPage) {
			this.optionsPerPage = Math.max(1, (this.height - optionsTop - 46) / OPTION_HEIGHT);
		}
		int pages = pageCount();
		this.optionPage = Math.min(this.optionPage, pages - 1);
		this.rebuildOptions();
	}

	private int pageCount() {
		return Math.max(1, (optionTexts.size() + optionsPerPage - 1) / optionsPerPage);
	}

	private int maxTextScroll() {
		return Math.max(0, lines.size() - textHeight / LINE_HEIGHT);
	}

	private void rebuildOptions() {
		this.buttons.clear();
		this.children.clear();
		int centerX = this.width / 2;
		int from = optionPage * optionsPerPage;
		int to = Math.min(optionTexts.size(), from + optionsPerPage);
		for (int i = from; i < to; i++) {
			final int index = i;
			final String full = optionTexts.get(i);
			int buttonWidth = Math.min(200, this.width - 10);
			int limit = buttonWidth - 16;
			boolean cut = this.font.width(full) > limit;
			TextComponent label = new TextComponent(cut ? this.font.plainSubstrByWidth(full, limit - this.font.width("...")) + "..." : full);
			Button.OnTooltip tooltip = (button, poseStack, mouseX, mouseY) -> {
				if (cut) {
					this.renderTooltip(poseStack, this.font.split(new TextComponent(full), Math.max(60, this.width / 2)), mouseX, mouseY);
				}
			};
			this.addButton(new Button(centerX - buttonWidth / 2, optionsTop + (i - from) * OPTION_HEIGHT, buttonWidth, 20, label,
					b -> ClientNet.send(new ChooseDialoguePayload(index)), tooltip));
		}
		if (pageCount() > 1) {
			int y = this.height - 26;
			previous = new Button(centerX - 100, y, 80, 20, new TranslatableComponent("npcdirector.gui.previous"), b -> changePage(-1));
			next = new Button(centerX + 20, y, 80, 20, new TranslatableComponent("npcdirector.gui.next"), b -> changePage(1));
			previous.active = optionPage > 0;
			next.active = optionPage < pageCount() - 1;
			this.addButton(previous);
			this.addButton(next);
		}
	}

	private void changePage(int delta) {
		int target = Math.max(0, Math.min(pageCount() - 1, optionPage + delta));
		if (target != optionPage) {
			optionPage = target;
			rebuildOptions();
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		int centerX = this.width / 2;
		int boxWidth = Math.max(60, Math.min(this.width - 40, 320));
		boolean overText = mouseX >= centerX - boxWidth / 2.0 && mouseX <= centerX + boxWidth / 2.0
				&& mouseY >= textTop && mouseY <= textTop + textHeight;
		if (overText && maxTextScroll() > 0) {
			textScroll = Math.max(0, Math.min(maxTextScroll(), textScroll - (int) Math.signum(delta)));
			return true;
		}
		if (pageCount() > 1) {
			changePage(delta < 0 ? 1 : -1);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(poseStack);
		int centerX = this.width / 2;
		int boxWidth = Math.max(60, Math.min(this.width - 40, 320));
		int visible = textHeight / LINE_HEIGHT;
		for (int i = 0; i < visible && textScroll + i < lines.size(); i++) {
			FormattedCharSequence line = lines.get(textScroll + i);
			this.font.drawShadow(poseStack, line, (float) (centerX - boxWidth / 2), (float) (textTop + i * LINE_HEIGHT), 0xFFFFFF);
		}
		if (maxTextScroll() > 0) {
			this.font.drawShadow(poseStack, (textScroll + 1) + "/" + (maxTextScroll() + 1), (float) (centerX + boxWidth / 2 - 24), (float) (textTop + textHeight + 2), 0xAAAAAA);
		}
		if (pageCount() > 1) {
			String pageLabel = (optionPage + 1) + "/" + pageCount();
			this.font.drawShadow(poseStack, pageLabel, (float) (centerX - this.font.width(pageLabel) / 2), (float) (this.height - 38), 0xAAAAAA);
		}
		super.render(poseStack, mouseX, mouseY, partialTick);
	}

	@Override
	public void onClose() {
		ClientNet.send(new CloseDialoguePayload());
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
