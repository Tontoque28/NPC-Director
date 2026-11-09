package com.tontoque28.npcdirector.client;

import java.util.List;

import com.tontoque28.npcdirector.network.ChooseDialoguePayload;
import com.tontoque28.npcdirector.network.CloseDialoguePayload;


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

public class DialogueScreen extends Screen {

	private static final int OPTION_HEIGHT = 22;
	private static final int LINE_HEIGHT = 10;

	private final String text;
	private final List<String> optionTexts;
	private List<FormattedCharSequence> lines = List.of();
	private int textTop;
	private int textHeight;
	private int textScroll;
	private int optionPage;
	private int optionsPerPage = 1;
	private int optionsTop;
	private Button previous;
	private Button next;

	public DialogueScreen(String text, List<String> optionTexts) {
		super(Component.literal(text));
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
		this.clearWidgets();
		int centerX = this.width / 2;
		int from = optionPage * optionsPerPage;
		int to = Math.min(optionTexts.size(), from + optionsPerPage);
		for (int i = from; i < to; i++) {
			int index = i;
			boolean cut = this.font.width(optionTexts.get(i)) > 184;
			Component label = Component.literal(cut ? this.font.plainSubstrByWidth(optionTexts.get(i), 184 - this.font.width("...")) + "..." : optionTexts.get(i));
			int buttonWidth = Math.min(200, this.width - 10);
			Button button = Button.builder(label, b -> ClientNet.send(new ChooseDialoguePayload(index)))
					.bounds(centerX - buttonWidth / 2, optionsTop + (i - from) * OPTION_HEIGHT, buttonWidth, 20)
					.build();
			if (cut) {
				button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(optionTexts.get(i))));
			}
			this.addRenderableWidget(button);
		}
		if (pageCount() > 1) {
			int y = this.height - 26;
			previous = Button.builder(Component.translatable("npcdirector.gui.previous"), b -> changePage(-1)).bounds(centerX - 100, y, 80, 20).build();
			next = Button.builder(Component.translatable("npcdirector.gui.next"), b -> changePage(1)).bounds(centerX + 20, y, 80, 20).build();
			previous.active = optionPage > 0;
			next.active = optionPage < pageCount() - 1;
			this.addRenderableWidget(previous);
			this.addRenderableWidget(next);
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
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
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
		return super.mouseScrolled(mouseX, mouseY, scrollX, delta);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		int centerX = this.width / 2;
		int boxWidth = Math.max(60, Math.min(this.width - 40, 320));
		int visible = textHeight / LINE_HEIGHT;
		for (int i = 0; i < visible && textScroll + i < lines.size(); i++) {
			FormattedCharSequence line = lines.get(textScroll + i);
			guiGraphics.drawString(this.font, line, centerX - boxWidth / 2, textTop + i * LINE_HEIGHT, 0xFFFFFF);
		}
		if (maxTextScroll() > 0) {
			guiGraphics.drawString(this.font, (textScroll + 1) + "/" + (maxTextScroll() + 1), centerX + boxWidth / 2 - 24, textTop + textHeight + 2, 0xAAAAAA);
		}
		if (pageCount() > 1) {
			String pageLabel = (optionPage + 1) + "/" + pageCount();
			guiGraphics.drawCenteredString(this.font, pageLabel, centerX, this.height - 38, 0xAAAAAA);
		}
		super.render(guiGraphics, mouseX, mouseY, partialTick);
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
