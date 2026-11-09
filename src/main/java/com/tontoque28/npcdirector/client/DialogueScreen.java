package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.network.ChooseDialoguePayload;
import com.tontoque28.npcdirector.network.CloseDialoguePayload;
import com.tontoque28.npcdirector.network.NetworkingInit;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DialogueScreen extends GuiScreen {

	private static final int OPTION_HEIGHT = 22;
	private static final int LINE_HEIGHT = 10;
	private static final int PREVIOUS_ID = 10000;
	private static final int NEXT_ID = 10001;
	private static final int BUTTON_TEXT_WIDTH = 184;

	private final String text;
	private final List<String> optionTexts;
	private final List<String> fullTexts = new ArrayList<>();
	private List<String> lines = Collections.emptyList();
	private int textTop;
	private int textHeight;
	private int textScroll;
	private int optionPage;
	private int optionsPerPage = 1;
	private int optionsTop;

	public DialogueScreen(String text, List<String> optionTexts) {
		this.text = text;
		this.optionTexts = optionTexts;
	}

	private int boxWidth() {
		return Math.max(60, Math.min(this.width - 40, 320));
	}

	@Override
	public void initGui() {
		this.lines = this.fontRenderer.listFormattedStringToWidth(text, boxWidth());
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
		this.optionPage = Math.min(this.optionPage, pageCount() - 1);
		this.rebuildOptions();
	}

	private int pageCount() {
		return Math.max(1, (optionTexts.size() + optionsPerPage - 1) / optionsPerPage);
	}

	private int maxTextScroll() {
		return Math.max(0, lines.size() - textHeight / LINE_HEIGHT);
	}

	private void rebuildOptions() {
		this.buttonList.clear();
		this.fullTexts.clear();
		int centerX = this.width / 2;
		int from = optionPage * optionsPerPage;
		int to = Math.min(optionTexts.size(), from + optionsPerPage);
		int buttonWidth = Math.min(200, this.width - 10);
		for (int i = from; i < to; i++) {
			String full = optionTexts.get(i);
			String label = full;
			if (this.fontRenderer.getStringWidth(full) > BUTTON_TEXT_WIDTH) {
				label = this.fontRenderer.trimStringToWidth(full, BUTTON_TEXT_WIDTH - this.fontRenderer.getStringWidth("...")) + "...";
			}
			this.buttonList.add(new GuiButton(i, centerX - buttonWidth / 2, optionsTop + (i - from) * OPTION_HEIGHT, buttonWidth, 20, label));
			this.fullTexts.add(full);
		}
		if (pageCount() > 1) {
			int y = this.height - 26;
			GuiButton previous = new GuiButton(PREVIOUS_ID, centerX - 100, y, 80, 20, I18n.format("npcdirector.gui.previous"));
			GuiButton next = new GuiButton(NEXT_ID, centerX + 20, y, 80, 20, I18n.format("npcdirector.gui.next"));
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
		int box = boxWidth();
		boolean overText = mouseX >= this.width / 2 - box / 2 && mouseX <= this.width / 2 + box / 2
				&& mouseY >= textTop && mouseY <= textTop + textHeight;
		if (overText && maxTextScroll() > 0) {
			textScroll = Math.max(0, Math.min(maxTextScroll(), textScroll + direction));
		} else if (pageCount() > 1) {
			changePage(direction);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		int centerX = this.width / 2;
		int box = boxWidth();
		int visible = textHeight / LINE_HEIGHT;
		for (int i = 0; i < visible && textScroll + i < lines.size(); i++) {
			this.fontRenderer.drawStringWithShadow(lines.get(textScroll + i), centerX - box / 2, textTop + i * LINE_HEIGHT, 0xFFFFFF);
		}
		if (maxTextScroll() > 0) {
			this.fontRenderer.drawStringWithShadow((textScroll + 1) + "/" + (maxTextScroll() + 1), centerX + box / 2 - 24, textTop + textHeight + 2, 0xAAAAAA);
		}
		if (pageCount() > 1) {
			this.drawCenteredString(this.fontRenderer, (optionPage + 1) + "/" + pageCount(), centerX, this.height - 38, 0xAAAAAA);
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
		for (GuiButton button : this.buttonList) {
			if (button.id < PREVIOUS_ID && mouseX >= button.x && mouseX < button.x + button.width && mouseY >= button.y && mouseY < button.y + button.height) {
				String full = optionTexts.get(button.id);
				if (this.fontRenderer.getStringWidth(full) > BUTTON_TEXT_WIDTH) {
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
