package com.tontoque28.npcdirector.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tontoque28.npcdirector.network.ChooseDialoguePayload;
import com.tontoque28.npcdirector.network.CloseDialoguePayload;
import com.tontoque28.npcdirector.network.NetworkingInit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class DialogueScreen extends GuiScreen {

	private static final int LINE_HEIGHT = 10;
	private static final int ROW_HEIGHT = 22;
	private static final int BUTTON_HEIGHT = 20;
	private static final int PAD = 8;
	private static final int HEADER_HEIGHT = 16;
	private static final int FOOTER_HEIGHT = 26;
	private static final int PREVIOUS_ID = 10000;
	private static final int NEXT_ID = 10001;
	private static final int CLOSE_ID = 10002;
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
	private List<String> lines = Collections.emptyList();
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

	public DialogueScreen(String text, List<String> optionTexts, String npcName) {
		this.text = text;
		this.optionTexts = optionTexts;
		this.npcName = npcName == null ? "" : npcName;
	}

	private int innerWidth() {
		return panelW - 2 * PAD;
	}

	@Override
	public void initGui() {
		panelW = Math.max(120, Math.min(this.width - 16, 330));
		this.lines = this.fontRenderer.listFormattedStringToWidth(text, innerWidth() - 8);
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

	private final class BeigeButton extends GuiButton {

		BeigeButton(int id, int x, int y, int width, int height, String label) {
			super(id, x, y, width, height, label);
		}

		@Override
		public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
			if (!this.visible) {
				return;
			}
			this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
			boolean hot = this.enabled && this.hovered;
			Gui.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, hot ? FRAME : FRAME_SHADE);
			Gui.drawRect(this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1, hot ? BUTTON_HOVER : BUTTON);
			if (hot) {
				Gui.drawRect(this.x, this.y, this.x + this.width, this.y + 1, 0xFF55B8FF);
				Gui.drawRect(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, 0xFF55B8FF);
				Gui.drawRect(this.x, this.y, this.x + 1, this.y + this.height, 0xFF55B8FF);
				Gui.drawRect(this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, 0xFF55B8FF);
			}
			int color = this.enabled ? BUTTON_TEXT : 0xFF8A877D;
			FontRendererHolder.draw(DialogueScreen.this, this.displayString, this.x, this.y, this.width, this.height, color);
		}
	}

	private static final class FontRendererHolder {
		static void draw(DialogueScreen screen, String label, int x, int y, int w, int h, int color) {
			int textWidth = screen.fontRenderer.getStringWidth(label);
			screen.fontRenderer.drawString(label, x + Math.max(2, (w - textWidth) / 2), y + (h - 8) / 2, color);
		}
	}

	private void rebuildOptions() {
		this.buttonList.clear();
		int gap = 4;
		int buttonWidth = (innerWidth() - gap) / 2;
		int from = optionPage * optionsPerPage;
		int to = Math.min(optionTexts.size(), from + optionsPerPage);
		for (int i = from; i < to; i++) {
			int slot = i - from;
			int column = slot % 2;
			int row = slot / 2;
			String full = optionTexts.get(i);
			String label = full;
			if (this.fontRenderer.getStringWidth(full) > buttonWidth - 8) {
				label = this.fontRenderer.trimStringToWidth(full, buttonWidth - 8 - this.fontRenderer.getStringWidth("...")) + "...";
			}
			this.buttonList.add(new BeigeButton(i, panelX + PAD + column * (buttonWidth + gap), optionsY + row * ROW_HEIGHT, buttonWidth, BUTTON_HEIGHT, label));
		}
		int footerY = panelY + panelH - PAD - BUTTON_HEIGHT;
		this.buttonList.add(new BeigeButton(CLOSE_ID, panelX + panelW - PAD - BUTTON_HEIGHT, footerY, BUTTON_HEIGHT, BUTTON_HEIGHT, "x"));
		if (pageCount() > 1) {
			BeigeButton previous = new BeigeButton(PREVIOUS_ID, panelX + PAD, footerY, 60, BUTTON_HEIGHT, ClientLang.tr("npcdirector.gui.previous"));
			BeigeButton next = new BeigeButton(NEXT_ID, panelX + PAD + 100, footerY, 60, BUTTON_HEIGHT, ClientLang.tr("npcdirector.gui.next"));
			previous.enabled = optionPage > 0;
			next.enabled = optionPage < pageCount() - 1;
			this.buttonList.add(previous);
			this.buttonList.add(next);
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
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == PREVIOUS_ID) {
			changePage(-1);
		} else if (button.id == NEXT_ID) {
			changePage(1);
		} else if (button.id == CLOSE_ID) {
			NetworkingInit.sendToServer(new CloseDialoguePayload());
			this.mc.displayGuiScreen(null);
		} else {
			NetworkingInit.sendToServer(new ChooseDialoguePayload(button.id));
		}
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int wheel = Mouse.getEventDWheel();
		if (wheel == 0) {
			return;
		}
		int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
		int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
		int direction = wheel > 0 ? -1 : 1;
		boolean overText = mouseX >= panelX + PAD && mouseX <= panelX + panelW - PAD && mouseY >= boxY && mouseY <= boxY + boxH;
		if (overText && maxTextScroll() > 0) {
			textScroll = Math.max(0, Math.min(maxTextScroll(), textScroll + direction));
		} else if (pageCount() > 1) {
			changePage(direction);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		Gui.drawRect(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, FRAME);
		Gui.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
		if (!npcName.isEmpty()) {
			this.fontRenderer.drawString(npcName, panelX + PAD, panelY + PAD + 2, TITLE);
		}
		int bx = panelX + PAD;
		int bw = innerWidth();
		Gui.drawRect(bx - 1, boxY - 1, bx + bw + 1, boxY + boxH + 1, FRAME);
		Gui.drawRect(bx, boxY, bx + bw, boxY + boxH, BOX);
		int visible = visibleLines();
		for (int i = 0; i < visible && textScroll + i < lines.size(); i++) {
			this.fontRenderer.drawString(lines.get(textScroll + i), bx + 4, boxY + 4 + i * LINE_HEIGHT, 0xFFFFFFFF);
		}
		if (maxTextScroll() > 0) {
			String counter = (textScroll + 1) + "/" + (maxTextScroll() + 1);
			this.fontRenderer.drawString(counter, bx + bw - 4 - this.fontRenderer.getStringWidth(counter), boxY + boxH - 10, 0xFFAAAAAA);
		}
		if (pageCount() > 1) {
			this.fontRenderer.drawString((optionPage + 1) + "/" + pageCount(), panelX + PAD + 78, panelY + panelH - PAD - 14, TITLE);
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
		for (GuiButton button : this.buttonList) {
			if (button.id < PREVIOUS_ID && mouseX >= button.x && mouseX < button.x + button.width && mouseY >= button.y && mouseY < button.y + button.height) {
				String full = optionTexts.get(button.id);
				if (this.fontRenderer.getStringWidth(full) > button.width - 8) {
					this.drawHoveringText(Collections.singletonList(full), mouseX, mouseY);
				}
			}
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			NetworkingInit.sendToServer(new CloseDialoguePayload());
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
