package com.tontoque28.npcdirector.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.tontoque28.npcdirector.network.ApplyAdminPayload;
import com.tontoque28.npcdirector.network.DeleteNpcPayload;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.network.SummonNpcPayload;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class AdminScreen extends GuiScreen {

	private static final String[] RELEVANCE_KEYS = {"none", "indirect", "main"};
	private static final int RELEVANCE_ID = 1;
	private static final int TARGETABLE_ID = 2;
	private static final int APPLY_ID = 3;
	private static final int CLOSE_ID = 4;
	private static final int DELETE_ID = 5;
	private static final int SUMMON_ID = 6;

	private final OpenAdminPayload data;
	private GuiTextField nameBox;
	private int relevance;
	private boolean targetable;
	private IdList pathList;
	private IdList dialogueList;
	private boolean deleteArmed;

	public AdminScreen(OpenAdminPayload data) {
		this.data = data;
		this.relevance = Math.max(0, Math.min(RELEVANCE_KEYS.length - 1, data.relevance()));
		this.targetable = data.targetable();
	}

	private boolean summonPanel() {
		return data.npcEntityId() < 0;
	}

	private int panelWidth() {
		return Math.max(120, Math.min(300, this.width - 10));
	}

	@Override
	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		int centerX = this.width / 2;
		if (summonPanel()) {
			this.buttonList.add(new GuiButton(SUMMON_ID, centerX - 100, this.height / 2 - 20, 200, 20, I18n.format("npcdirector.gui.summon")));
			this.buttonList.add(new GuiButton(CLOSE_ID, centerX - 100, this.height / 2 + 6, 200, 20, I18n.format("npcdirector.gui.close")));
			return;
		}
		String previousName = nameBox == null ? data.name() : nameBox.getText();
		int panel = panelWidth();
		int half = (panel - 4) / 2;
		int third = (panel - 8) / 3;
		int left = centerX - panel / 2;
		nameBox = new GuiTextField(0, this.fontRenderer, left, 34, panel, 18);
		nameBox.setMaxStringLength(64);
		nameBox.setText(previousName);
		nameBox.setFocused(true);
		this.buttonList.add(new GuiButton(RELEVANCE_ID, left, 58, half, 20, relevanceLabel()));
		this.buttonList.add(new GuiButton(TARGETABLE_ID, left + half + 4, 58, half, 20, targetableLabel()));
		int listTop = 98;
		int listBottom = Math.max(listTop + 40, this.height - 56);
		String selectedPath = pathList == null ? data.pathId() : pathList.selectedId();
		String selectedDialogue = dialogueList == null ? data.dialogueId() : dialogueList.selectedId();
		pathList = new IdList(left, listTop, half, listBottom - listTop, data.paths(), selectedPath);
		dialogueList = new IdList(left + half + 4, listTop, half, listBottom - listTop, data.dialogues(), selectedDialogue);
		int buttonY = this.height - 28;
		this.buttonList.add(new GuiButton(APPLY_ID, left, buttonY, third, 20, I18n.format("npcdirector.gui.apply")));
		this.buttonList.add(new GuiButton(CLOSE_ID, left + third + 4, buttonY, third, 20, I18n.format("npcdirector.gui.close")));
		this.buttonList.add(new GuiButton(DELETE_ID, left + 2 * (third + 4), buttonY, third, 20,
				I18n.format(deleteArmed ? "npcdirector.gui.delete_confirm" : "npcdirector.gui.delete")));
	}

	private String relevanceLabel() {
		return I18n.format("npcdirector.gui.relevance", I18n.format("npcdirector.gui.relevance." + RELEVANCE_KEYS[relevance]));
	}

	private String targetableLabel() {
		return I18n.format("npcdirector.gui.targetable", I18n.format(targetable ? "npcdirector.gui.yes" : "npcdirector.gui.no"));
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		switch (button.id) {
			case RELEVANCE_ID:
				relevance = (relevance + 1) % RELEVANCE_KEYS.length;
				button.displayString = relevanceLabel();
				return;
			case TARGETABLE_ID:
				targetable = !targetable;
				button.displayString = targetableLabel();
				return;
			case APPLY_ID:
				NetworkingInit.sendToServer(new ApplyAdminPayload(data.npcEntityId(), nameBox.getText(), relevance, targetable,
						pathList.selectedId(), dialogueList.selectedId()));
				this.mc.displayGuiScreen(null);
				return;
			case CLOSE_ID:
				this.mc.displayGuiScreen(null);
				return;
			case DELETE_ID:
				if (!deleteArmed) {
					deleteArmed = true;
					button.displayString = I18n.format("npcdirector.gui.delete_confirm");
					return;
				}
				NetworkingInit.sendToServer(new DeleteNpcPayload(data.npcEntityId()));
				this.mc.displayGuiScreen(null);
				return;
			case SUMMON_ID:
				NetworkingInit.sendToServer(new SummonNpcPayload());
				this.mc.displayGuiScreen(null);
				return;
			default:
				return;
		}
	}

	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	public void updateScreen() {
		if (nameBox != null) {
			nameBox.updateCursorCounter();
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode != Keyboard.KEY_ESCAPE && nameBox != null && nameBox.textboxKeyTyped(typedChar, keyCode)) {
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (nameBox != null) {
			nameBox.mouseClicked(mouseX, mouseY, mouseButton);
			pathList.click(mouseX, mouseY, mouseButton);
			dialogueList.click(mouseX, mouseY, mouseButton);
		}
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int wheel = Mouse.getEventDWheel();
		if (wheel == 0 || nameBox == null) {
			return;
		}
		int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
		int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
		int direction = wheel > 0 ? -1 : 1;
		pathList.scroll(mouseX, mouseY, direction);
		dialogueList.scroll(mouseX, mouseY, direction);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		String title = I18n.format(summonPanel() ? "npcdirector.gui.summon_title" : "npcdirector.gui.title");
		this.drawCenteredString(this.fontRenderer, title, this.width / 2, 12, 0xFFFFFF);
		if (!summonPanel()) {
			int panel = panelWidth();
			int half = (panel - 4) / 2;
			int left = this.width / 2 - panel / 2;
			this.fontRenderer.drawStringWithShadow(I18n.format("npcdirector.gui.paths"), left, 86, 0xAAAAAA);
			this.fontRenderer.drawStringWithShadow(I18n.format("npcdirector.gui.dialogues"), left + half + 4, 86, 0xAAAAAA);
			nameBox.drawTextBox();
			pathList.draw();
			dialogueList.draw();
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	private class IdList {

		private static final int ROW = 16;
		private final int x;
		private final int y;
		private final int width;
		private final int height;
		private final List<String> entries = new ArrayList<>();
		private int selected;
		private int scroll;

		IdList(int x, int y, int width, int height, List<String> ids, String selectedId) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			entries.add("");
			entries.addAll(ids);
			selected = entries.indexOf(selectedId);
			if (selected < 0 && !selectedId.isEmpty()) {
				entries.add(selectedId);
				selected = entries.size() - 1;
			}
			if (selected < 0) {
				selected = 0;
			}
		}

		String selectedId() {
			return selected >= 0 && selected < entries.size() ? entries.get(selected) : "";
		}

		private int visibleRows() {
			return Math.max(1, height / ROW);
		}

		private int maxScroll() {
			return Math.max(0, entries.size() - visibleRows());
		}

		private boolean inside(int mouseX, int mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}

		void draw() {
			Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF808080);
			Gui.drawRect(x, y, x + width, y + height, 0xFF000000);
			scroll = Math.max(0, Math.min(maxScroll(), scroll));
			int rows = visibleRows();
			for (int i = 0; i < rows && scroll + i < entries.size(); i++) {
				int index = scroll + i;
				String id = entries.get(index);
				String label = id.isEmpty() ? I18n.format("npcdirector.gui.none") : id;
				String shown = AdminScreen.this.fontRenderer.trimStringToWidth(label, width - 10);
				int rowY = y + i * ROW;
				if (index == selected) {
					Gui.drawRect(x, rowY, x + width - 6, rowY + ROW, 0xFF404040);
				}
				AdminScreen.this.fontRenderer.drawStringWithShadow(shown, x + 2, rowY + 4, index == selected ? 0xFFFF55 : 0xFFFFFF);
			}
			if (maxScroll() > 0) {
				int barHeight = Math.max(8, height * rows / entries.size());
				int barY = y + (height - barHeight) * scroll / maxScroll();
				Gui.drawRect(x + width - 5, barY, x + width - 1, barY + barHeight, 0xFFC0C0C0);
			}
		}

		void click(int mouseX, int mouseY, int button) {
			if (button != 0 || !inside(mouseX, mouseY)) {
				return;
			}
			int index = scroll + (mouseY - y) / ROW;
			if (index >= 0 && index < entries.size()) {
				selected = index;
			}
		}

		void scroll(int mouseX, int mouseY, int direction) {
			if (inside(mouseX, mouseY)) {
				scroll = Math.max(0, Math.min(maxScroll(), scroll + direction));
			}
		}
	}
}
