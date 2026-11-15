package com.tontoque28.npcdirector.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.tontoque28.npcdirector.network.ApplyAdminPayload;
import com.tontoque28.npcdirector.network.AssignPathPayload;
import com.tontoque28.npcdirector.network.DeleteNpcPayload;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.network.SummonNpcPayload;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class AdminScreen extends GuiScreen {

	private static final String[] RELEVANCE_KEYS = {"none", "indirect", "main"};
	private static final float[] SPEEDS = {0.5F, 0.75F, 1.0F, 1.5F, 2.0F};
	private static final int RELEVANCE_ID = 1;
	private static final int TARGETABLE_ID = 2;
	private static final int APPLY_ID = 3;
	private static final int CLOSE_ID = 4;
	private static final int DELETE_ID = 5;
	private static final int SUMMON_ID = 6;
	private static final int LANG_ID = 7;
	private static final int TAB_GENERAL_ID = 8;
	private static final int TAB_POSITION_ID = 9;
	private static final int PATH_PREV_ID = 10;
	private static final int PATH_NEXT_ID = 11;
	private static final int ASSIGN_THIS_ID = 12;
	private static final int ASSIGN_RADIUS_ID = 13;
	private static final int LOOK_ID = 14;
	private static final int STILL_ID = 15;
	private static final int INVULNERABLE_ID = 16;
	private static final int SEATED_ID = 17;
	private static final int SPEED_ID = 18;
	private static final int SKIN_IMPORT_ID = 19;
	private static final int SKIN_CLEAR_ID = 20;

	private final OpenAdminPayload data;
	private GuiTextField nameBox;
	private int relevance;
	private boolean targetable;
	private boolean lookAtPlayer;
	private boolean stationary;
	private boolean invulnerable;
	private boolean seated;
	private int speedIndex;
	private IdList dialogueList;
	private final List<String> pathIds = new ArrayList<>();
	private int pathIndex;
	private int tab;
	private boolean deleteArmed;
	private String nameValue;
	private String message = "";
	private long messageUntil;

	public AdminScreen(OpenAdminPayload data) {
		this.data = data;
		this.nameValue = data.name();
		this.relevance = Math.max(0, Math.min(RELEVANCE_KEYS.length - 1, data.relevance()));
		this.targetable = data.targetable();
		this.lookAtPlayer = data.lookAtPlayer();
		this.stationary = data.stationary();
		this.invulnerable = data.invulnerable();
		this.seated = data.seated();
		this.speedIndex = nearestSpeed(data.patrolSpeed());
		pathIds.add("");
		pathIds.addAll(data.paths());
		pathIndex = pathIds.indexOf(data.pathId());
		if (pathIndex < 0 && !data.pathId().isEmpty()) {
			pathIds.add(data.pathId());
			pathIndex = pathIds.size() - 1;
		}
		if (pathIndex < 0) {
			pathIndex = 0;
		}
	}

	private static int nearestSpeed(float value) {
		int best = 2;
		float distance = Float.MAX_VALUE;
		for (int i = 0; i < SPEEDS.length; i++) {
			float d = Math.abs(SPEEDS[i] - value);
			if (d < distance) {
				distance = d;
				best = i;
			}
		}
		return best;
	}

	public void showMessage(String key) {
		this.message = ClientLang.tr(key);
		this.messageUntil = System.currentTimeMillis() + 8000L;
	}

	private boolean summonPanel() {
		return data.npcEntityId() < 0;
	}

	private int panelWidth() {
		return Math.max(120, Math.min(300, this.width - 10));
	}

	private String selectedPath() {
		return pathIds.get(pathIndex);
	}

	@Override
	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		this.buttonList.clear();
		int centerX = this.width / 2;
		this.buttonList.add(new GuiButton(LANG_ID, this.width - 104, 4, 100, 20, ClientLang.label()));
		if (summonPanel()) {
			this.buttonList.add(new GuiButton(SUMMON_ID, centerX - 100, this.height / 2 - 20, 200, 20, ClientLang.tr("npcdirector.gui.summon")));
			this.buttonList.add(new GuiButton(CLOSE_ID, centerX - 100, this.height / 2 + 6, 200, 20, ClientLang.tr("npcdirector.gui.close")));
			return;
		}
		String previousName = nameValue;
		int panel = panelWidth();
		int half = (panel - 4) / 2;
		int third = (panel - 8) / 3;
		int left = centerX - panel / 2;
		GuiButton generalTab = new GuiButton(TAB_GENERAL_ID, left, 26, half, 20, ClientLang.tr("npcdirector.gui.tab.general"));
		GuiButton positionTab = new GuiButton(TAB_POSITION_ID, left + half + 4, 26, half, 20, ClientLang.tr("npcdirector.gui.tab.position"));
		generalTab.enabled = tab != 0;
		positionTab.enabled = tab != 1;
		this.buttonList.add(generalTab);
		this.buttonList.add(positionTab);
		nameBox = null;
		if (tab == 0) {
			nameBox = new GuiTextField(0, this.fontRenderer, left, 66, panel, 18);
			nameBox.setMaxStringLength(64);
			nameBox.setText(previousName);
			nameBox.setFocused(true);
			this.buttonList.add(new GuiButton(RELEVANCE_ID, left, 90, half, 20, relevanceLabel()));
			this.buttonList.add(new GuiButton(TARGETABLE_ID, left + half + 4, 90, half, 20, toggle("npcdirector.gui.targetable", targetable)));
			int listTop = 128;
			int listBottom = Math.max(listTop + 30, this.height - 76);
			String selectedDialogue = dialogueList == null ? data.dialogueId() : dialogueList.selectedId();
			dialogueList = new IdList(left, listTop, panel, listBottom - listTop, data.dialogues(), selectedDialogue);
			int skinY = listBottom + 4;
			this.buttonList.add(new GuiButton(SKIN_IMPORT_ID, left, skinY, half + 30, 20, ClientLang.tr("npcdirector.gui.skin_import")));
			this.buttonList.add(new GuiButton(SKIN_CLEAR_ID, left + half + 34, skinY, panel - half - 34, 20, ClientLang.tr("npcdirector.gui.skin_clear")));
		} else {
			if (dialogueList == null) {
				dialogueList = new IdList(0, 0, 10, 10, data.dialogues(), data.dialogueId());
			}
			this.buttonList.add(new GuiButton(PATH_PREV_ID, left, 66, 20, 20, "<"));
			this.buttonList.add(new GuiButton(PATH_NEXT_ID, left + panel - 20, 66, 20, 20, ">"));
			this.buttonList.add(new GuiButton(ASSIGN_THIS_ID, left, 90, half, 20, ClientLang.tr("npcdirector.gui.assign_this")));
			this.buttonList.add(new GuiButton(ASSIGN_RADIUS_ID, left + half + 4, 90, half, 20, ClientLang.tr("npcdirector.gui.assign_radius")));
			this.buttonList.add(new GuiButton(LOOK_ID, left, 124, half, 20, toggle("npcdirector.gui.behavior.look", lookAtPlayer)));
			this.buttonList.add(new GuiButton(STILL_ID, left + half + 4, 124, half, 20, toggle("npcdirector.gui.behavior.still", stationary)));
			this.buttonList.add(new GuiButton(INVULNERABLE_ID, left, 148, half, 20, toggle("npcdirector.gui.behavior.invulnerable", invulnerable)));
			this.buttonList.add(new GuiButton(SEATED_ID, left + half + 4, 148, half, 20, toggle("npcdirector.gui.behavior.seated", seated)));
			this.buttonList.add(new GuiButton(SPEED_ID, left, 172, panel, 20, speedLabel()));
		}
		int buttonY = this.height - 28;
		this.buttonList.add(new GuiButton(APPLY_ID, left, buttonY, third, 20, ClientLang.tr("npcdirector.gui.apply")));
		this.buttonList.add(new GuiButton(CLOSE_ID, left + third + 4, buttonY, third, 20, ClientLang.tr("npcdirector.gui.close")));
		this.buttonList.add(new GuiButton(DELETE_ID, left + 2 * (third + 4), buttonY, third, 20,
				ClientLang.tr(deleteArmed ? "npcdirector.gui.delete_confirm" : "npcdirector.gui.delete")));
	}

	private String relevanceLabel() {
		return ClientLang.tr("npcdirector.gui.relevance", ClientLang.tr("npcdirector.gui.relevance." + RELEVANCE_KEYS[relevance]));
	}

	private String toggle(String key, boolean value) {
		return ClientLang.tr(key, ClientLang.tr(value ? "npcdirector.gui.yes" : "npcdirector.gui.no"));
	}

	private String speedLabel() {
		return ClientLang.tr("npcdirector.gui.behavior.speed", Float.toString(SPEEDS[speedIndex]));
	}

	private void sendApply() {
		captureName();
		String name = nameValue;
		NetworkingInit.sendToServer(new ApplyAdminPayload(data.npcEntityId(), name, relevance, targetable,
				selectedPath(), dialogueList.selectedId(), lookAtPlayer, stationary, invulnerable, seated, SPEEDS[speedIndex]));
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		switch (button.id) {
			case LANG_ID:
				captureName();
				ClientLang.cycle();
				this.initGui();
				return;
			case TAB_GENERAL_ID:
				captureName();
				tab = 0;
				this.initGui();
				return;
			case TAB_POSITION_ID:
				captureName();
				tab = 1;
				this.initGui();
				return;
			case RELEVANCE_ID:
				relevance = (relevance + 1) % RELEVANCE_KEYS.length;
				button.displayString = relevanceLabel();
				return;
			case TARGETABLE_ID:
				targetable = !targetable;
				button.displayString = toggle("npcdirector.gui.targetable", targetable);
				return;
			case LOOK_ID:
				lookAtPlayer = !lookAtPlayer;
				button.displayString = toggle("npcdirector.gui.behavior.look", lookAtPlayer);
				return;
			case STILL_ID:
				stationary = !stationary;
				button.displayString = toggle("npcdirector.gui.behavior.still", stationary);
				return;
			case INVULNERABLE_ID:
				invulnerable = !invulnerable;
				button.displayString = toggle("npcdirector.gui.behavior.invulnerable", invulnerable);
				return;
			case SEATED_ID:
				seated = !seated;
				button.displayString = toggle("npcdirector.gui.behavior.seated", seated);
				return;
			case SPEED_ID:
				speedIndex = (speedIndex + 1) % SPEEDS.length;
				button.displayString = speedLabel();
				return;
			case PATH_PREV_ID:
				pathIndex = (pathIndex + pathIds.size() - 1) % pathIds.size();
				return;
			case PATH_NEXT_ID:
				pathIndex = (pathIndex + 1) % pathIds.size();
				return;
			case ASSIGN_THIS_ID:
				NetworkingInit.sendToServer(new AssignPathPayload(data.npcEntityId(), selectedPath(), false));
				return;
			case ASSIGN_RADIUS_ID:
				NetworkingInit.sendToServer(new AssignPathPayload(data.npcEntityId(), selectedPath(), true));
				return;
			case SKIN_IMPORT_ID:
				SkinImport.pick(data.npcEntityId());
				showMessage("npcdirector.skin.import_picking");
				return;
			case SKIN_CLEAR_ID:
				SkinImport.clear(data.npcEntityId());
				return;
			case APPLY_ID:
				sendApply();
				this.mc.displayGuiScreen(null);
				return;
			case CLOSE_ID:
				this.mc.displayGuiScreen(null);
				return;
			case DELETE_ID:
				if (!deleteArmed) {
					deleteArmed = true;
					button.displayString = ClientLang.tr("npcdirector.gui.delete_confirm");
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

	private void captureName() {
		if (nameBox != null) {
			nameValue = nameBox.getText();
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
		dialogueList.scroll(mouseX, mouseY, wheel > 0 ? -1 : 1);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		String title = ClientLang.tr(summonPanel() ? "npcdirector.gui.summon_title" : "npcdirector.gui.title");
		this.drawCenteredString(this.fontRenderer, title, this.width / 2, 10, 0xFFFFFF);
		if (!summonPanel()) {
			int panel = panelWidth();
			int left = this.width / 2 - panel / 2;
			if (tab == 0) {
				this.fontRenderer.drawStringWithShadow(ClientLang.tr("npcdirector.gui.name"), left, 54, 0xAAAAAA);
				this.fontRenderer.drawStringWithShadow(ClientLang.tr("npcdirector.gui.dialogues"), left, 116, 0xAAAAAA);
				nameBox.drawTextBox();
				dialogueList.draw();
			} else {
				String label = selectedPath().isEmpty() ? ClientLang.tr("npcdirector.gui.none") : selectedPath();
				this.fontRenderer.drawStringWithShadow(ClientLang.tr("npcdirector.gui.paths"), left, 54, 0xAAAAAA);
				Gui.drawRect(left + 22, 66, left + panel - 22, 86, 0xFF808080);
				Gui.drawRect(left + 23, 67, left + panel - 23, 85, 0xFF000000);
				this.drawCenteredString(this.fontRenderer, this.fontRenderer.trimStringToWidth(label, panel - 50), this.width / 2, 72, 0xFFFF55);
				this.fontRenderer.drawStringWithShadow(ClientLang.tr("npcdirector.gui.behavior"), left, 112, 0xAAAAAA);
			}
			if (System.currentTimeMillis() < messageUntil) {
				this.drawCenteredString(this.fontRenderer, this.fontRenderer.trimStringToWidth(message, this.width - 10), this.width / 2, this.height - 40, 0xFFFF55);
			}
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (summonPanel() && System.currentTimeMillis() < messageUntil) {
			this.drawCenteredString(this.fontRenderer, message, this.width / 2, this.height / 2 + 36, 0xFFFF55);
		}
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
				String label = id.isEmpty() ? ClientLang.tr("npcdirector.gui.none") : id;
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
