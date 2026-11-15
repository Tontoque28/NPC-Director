package com.tontoque28.npcdirector.client;

import java.util.List;

import com.tontoque28.npcdirector.network.ChooseDialoguePayload;
import com.tontoque28.npcdirector.network.CloseDialoguePayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
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
	private List<FormattedCharSequence> lines = List.of();
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
		super(Component.literal(text));
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

	private final class BeigeButton extends HoverButton {

		BeigeButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
			super(x, y, width, height, message, onPress);
		}

		@Override
		public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			int x = this.getX();
			int y = this.getY();
			int w = this.getWidth();
			int h = this.getHeight();
			boolean hot = this.active && this.isHoveredOrFocused();
			g.fill(x, y, x + w, y + h, hot ? FRAME : FRAME_SHADE);
			g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hot ? BUTTON_HOVER : BUTTON);
			if (hot) {
				g.renderOutline(x, y, w, h, 0xFF55B8FF);
			}
			Component label = this.getMessage();
			int color = this.active ? BUTTON_TEXT : 0xFF8A877D;
			int textWidth = DialogueScreen.this.font.width(label);
			g.drawString(DialogueScreen.this.font, label, x + Math.max(2, (w - textWidth) / 2), y + (h - 8) / 2, color, false);
		}
	}

	private Button beige(Component label, int x, int y, int w, int h, Button.OnPress action) {
		return new BeigeButton(x, y, w, h, label, action);
	}

	private void rebuildOptions() {
		this.clearWidgets();
		int gap = 4;
		int buttonWidth = (innerWidth() - gap) / 2;
		int from = optionPage * optionsPerPage;
		int to = Math.min(optionTexts.size(), from + optionsPerPage);
		for (int i = from; i < to; i++) {
			int index = i;
			int slot = i - from;
			int column = slot % 2;
			int row = slot / 2;
			String full = optionTexts.get(i);
			boolean cut = this.font.width(full) > buttonWidth - 8;
			Component label = Component.literal(cut ? this.font.plainSubstrByWidth(full, buttonWidth - 8 - this.font.width("...")) + "..." : full);
			Button button = beige(label, panelX + PAD + column * (buttonWidth + gap), optionsY + row * ROW_HEIGHT, buttonWidth, BUTTON_HEIGHT,
					b -> ClientNet.send(new ChooseDialoguePayload(index)));
			if (cut) {
				button.setTooltip(Tooltip.create(Component.literal(full)));
			}
			this.addRenderableWidget(button);
		}
		int footerY = panelY + panelH - PAD - BUTTON_HEIGHT;
		Button close = beige(Component.literal("x"), panelX + panelW - PAD - BUTTON_HEIGHT, footerY, BUTTON_HEIGHT, BUTTON_HEIGHT, b -> this.onClose());
		close.setTooltip(Tooltip.create(ClientLang.tr("npcdirector.gui.close")));
		this.addRenderableWidget(close);
		if (pageCount() > 1) {
			previous = beige(ClientLang.tr("npcdirector.gui.previous"), panelX + PAD, footerY, 60, BUTTON_HEIGHT, b -> changePage(-1));
			next = beige(ClientLang.tr("npcdirector.gui.next"), panelX + PAD + 100, footerY, 60, BUTTON_HEIGHT, b -> changePage(1));
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
		boolean overText = mouseX >= panelX + PAD && mouseX <= panelX + panelW - PAD && mouseY >= boxY && mouseY <= boxY + boxH;
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
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, FRAME);
		g.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
		if (!npcName.isEmpty()) {
			g.drawString(this.font, npcName, panelX + PAD, panelY + PAD + 2, TITLE, false);
		}
		int bx = panelX + PAD;
		int bw = innerWidth();
		g.fill(bx - 1, boxY - 1, bx + bw + 1, boxY + boxH + 1, FRAME);
		g.fill(bx, boxY, bx + bw, boxY + boxH, BOX);
		int visible = visibleLines();
		for (int i = 0; i < visible && textScroll + i < lines.size(); i++) {
			g.drawString(this.font, lines.get(textScroll + i), bx + 4, boxY + 4 + i * LINE_HEIGHT, 0xFFFFFFFF);
		}
		if (maxTextScroll() > 0) {
			String counter = (textScroll + 1) + "/" + (maxTextScroll() + 1);
			g.drawString(this.font, counter, bx + bw - 4 - this.font.width(counter), boxY + boxH - 10, 0xFFAAAAAA);
		}
		if (pageCount() > 1) {
			String pageLabel = (optionPage + 1) + "/" + pageCount();
			g.drawString(this.font, pageLabel, panelX + PAD + 78, panelY + panelH - PAD - 14, TITLE, false);
		}
		super.render(g, mouseX, mouseY, partialTick);
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
