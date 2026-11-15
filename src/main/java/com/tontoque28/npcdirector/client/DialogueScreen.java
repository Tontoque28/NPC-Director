package com.tontoque28.npcdirector.client;

import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tontoque28.npcdirector.network.ChooseDialoguePayload;
import com.tontoque28.npcdirector.network.CloseDialoguePayload;

import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;

public class DialogueScreen extends Screen {

	private static final int LINE_HEIGHT = 10;
	private static final int ROW_HEIGHT = 22;
	private static final int BUTTON_HEIGHT = 20;
	private static final int PAD = 8;
	private static final int HEADER_HEIGHT = 16;
	private static final int FOOTER_HEIGHT = 26;
	private static final int FRAME = 0xFFF1ECDF;
	private static final int FRAME_SHADE = 0xFF8E897D;
	private static final int PANEL = 0xE6C4BFB2;
	private static final int TITLE = 0xFF3B3830;
	private static final int BOX = 0x99000000;
	private static final int BUTTON = 0xFFD8D3C6;
	private static final int BUTTON_HOVER = 0xFFE9E4D6;
	private static final int BUTTON_TEXT = 0xFF35322B;

	private final String text;
	private final List<String> optionTexts;
	private final String npcName;
	private List<FormattedCharSequence> lines = Collections.emptyList();
	private int panelX;
	private int panelY;
	private int panelW;
	private int panelH;
	private int boxY;
	private int boxH;
	private int optionsY;
	private int textScroll;
	private int optionPage;
	private int optionsPerPage = 2;
	private Button previous;
	private Button next;

	public DialogueScreen(String text, List<String> optionTexts, String npcName) {
		super(new TextComponent(text));
		this.text = text;
		this.optionTexts = optionTexts;
		this.npcName = npcName == null ? "" : npcName;
	}

	private int innerWidth() {
		return panelW - 2 * PAD;
	}

	@Override
	protected void init() {
		panelW = Math.max(120, Math.min(this.width - 16, 330));
		this.lines = this.font.split(FormattedText.of(text), innerWidth() - 8);
		int maxBox = Math.max(LINE_HEIGHT * 3 + 8, (this.height - 80) / 2);
		this.boxH = Math.min(lines.size() * LINE_HEIGHT + 8, maxBox);
		int fixed = PAD + HEADER_HEIGHT + boxH + PAD + FOOTER_HEIGHT + PAD;
		int rowsFit = Math.max(1, (this.height - 8 - fixed) / ROW_HEIGHT);
		this.optionsPerPage = rowsFit * 2;
		int shown = Math.min(optionTexts.size(), optionsPerPage);
		int rows = Math.max(1, (shown + 1) / 2);
		this.panelH = fixed + rows * ROW_HEIGHT;
		this.panelX = (this.width - panelW) / 2;
		this.panelY = Math.max(4, (this.height - panelH) / 2);
		this.boxY = panelY + PAD + HEADER_HEIGHT;
		this.optionsY = boxY + boxH + PAD;
		this.textScroll = Math.min(this.textScroll, maxTextScroll());
		this.optionPage = Math.min(this.optionPage, pageCount() - 1);
		this.rebuildOptions();
	}

	private int pageCount() {
		return Math.max(1, (optionTexts.size() + optionsPerPage - 1) / optionsPerPage);
	}

	private int visibleLines() {
		return Math.max(1, (boxH - 8) / LINE_HEIGHT);
	}

	private int maxTextScroll() {
		return Math.max(0, lines.size() - visibleLines());
	}

	private final class BeigeButton extends Button {

		private final Component tip;

		BeigeButton(int x, int y, int width, int height, Component message, Button.OnPress onPress, Component tip) {
			super(x, y, width, height, message, onPress);
			this.tip = tip;
		}

		Component tip() {
			return tip;
		}

		@Override
		public void renderButton(PoseStack ps, int mouseX, int mouseY, float partialTick) {
			int x = this.x;
			int y = this.y;
			int w = this.getWidth();
			int h = this.getHeight();
			boolean hot = this.active && this.isHovered();
			GuiComponent.fill(ps, x, y, x + w, y + h, hot ? FRAME : FRAME_SHADE);
			GuiComponent.fill(ps, x + 1, y + 1, x + w - 1, y + h - 1, hot ? BUTTON_HOVER : BUTTON);
			if (hot) {
				GuiComponent.fill(ps, x, y, x + w, y + 1, 0xFF55B8FF);
				GuiComponent.fill(ps, x, y + h - 1, x + w, y + h, 0xFF55B8FF);
				GuiComponent.fill(ps, x, y, x + 1, y + h, 0xFF55B8FF);
				GuiComponent.fill(ps, x + w - 1, y, x + w, y + h, 0xFF55B8FF);
			}
			Component label = this.getMessage();
			int color = this.active ? BUTTON_TEXT : 0xFF8A877D;
			int textWidth = DialogueScreen.this.font.width(label);
			DialogueScreen.this.font.draw(ps, label, (float) (x + Math.max(2, (w - textWidth) / 2)), (float) (y + (h - 8) / 2), color);
		}
	}

	private Button beige(Component label, int x, int y, int w, int h, Button.OnPress action, Component tip) {
		return new BeigeButton(x, y, w, h, label, action, tip);
	}

	private void rebuildOptions() {
		this.buttons.clear();
		this.children.clear();
		int gap = 4;
		int buttonWidth = (innerWidth() - gap) / 2;
		int from = optionPage * optionsPerPage;
		int to = Math.min(optionTexts.size(), from + optionsPerPage);
		for (int i = from; i < to; i++) {
			final int index = i;
			int slot = i - from;
			int column = slot % 2;
			int row = slot / 2;
			String full = optionTexts.get(i);
			boolean cut = this.font.width(full) > buttonWidth - 8;
			Component label = new TextComponent(cut ? this.font.plainSubstrByWidth(full, buttonWidth - 8 - this.font.width("...")) + "..." : full);
			Button button = beige(label, panelX + PAD + column * (buttonWidth + gap), optionsY + row * ROW_HEIGHT, buttonWidth, BUTTON_HEIGHT,
					b -> ClientNet.send(new ChooseDialoguePayload(index)), cut ? new TextComponent(full) : null);
			this.addButton(button);
		}
		int footerY = panelY + panelH - PAD - BUTTON_HEIGHT;
		Button close = beige(new TextComponent("x"), panelX + panelW - PAD - BUTTON_HEIGHT, footerY, BUTTON_HEIGHT, BUTTON_HEIGHT, b -> this.onClose(),
				ClientLang.tr("npcdirector.gui.close"));
		this.addButton(close);
		if (pageCount() > 1) {
			previous = beige(ClientLang.tr("npcdirector.gui.previous"), panelX + PAD, footerY, 60, BUTTON_HEIGHT, b -> changePage(-1), null);
			next = beige(ClientLang.tr("npcdirector.gui.next"), panelX + PAD + 100, footerY, 60, BUTTON_HEIGHT, b -> changePage(1), null);
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
		boolean overText = mouseX >= panelX + PAD && mouseX <= panelX + panelW - PAD && mouseY >= boxY && mouseY <= boxY + boxH;
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
	public void render(PoseStack ps, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(ps);
		GuiComponent.fill(ps, panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, FRAME);
		GuiComponent.fill(ps, panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
		if (!npcName.isEmpty()) {
			this.font.draw(ps, npcName, (float) (panelX + PAD), (float) (panelY + PAD + 2), TITLE);
		}
		int bx = panelX + PAD;
		int bw = innerWidth();
		GuiComponent.fill(ps, bx - 1, boxY - 1, bx + bw + 1, boxY + boxH + 1, FRAME);
		GuiComponent.fill(ps, bx, boxY, bx + bw, boxY + boxH, BOX);
		int visible = visibleLines();
		for (int i = 0; i < visible && textScroll + i < lines.size(); i++) {
			this.font.drawShadow(ps, lines.get(textScroll + i), (float) (bx + 4), (float) (boxY + 4 + i * LINE_HEIGHT), 0xFFFFFFFF);
		}
		if (maxTextScroll() > 0) {
			String counter = (textScroll + 1) + "/" + (maxTextScroll() + 1);
			this.font.draw(ps, counter, (float) (bx + bw - 4 - this.font.width(counter)), (float) (boxY + boxH - 10), 0xFFAAAAAA);
		}
		if (pageCount() > 1) {
			String pageLabel = (optionPage + 1) + "/" + pageCount();
			this.font.draw(ps, pageLabel, (float) (panelX + PAD + 78), (float) (panelY + panelH - PAD - 14), TITLE);
		}
		super.render(ps, mouseX, mouseY, partialTick);
		for (net.minecraft.client.gui.components.AbstractWidget widget : this.buttons) {
			if (widget instanceof BeigeButton && widget.isHovered()) {
				Component tip = ((BeigeButton) widget).tip();
				if (tip != null) {
					this.renderTooltip(ps, this.font.split(tip, Math.max(60, this.width / 2)), mouseX, mouseY);
				}
				break;
			}
		}
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
