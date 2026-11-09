package com.tontoque28.npcdirector.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.tontoque28.npcdirector.network.ApplyAdminPayload;
import com.tontoque28.npcdirector.network.DeleteNpcPayload;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.network.SummonNpcPayload;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class AdminScreen extends Screen {

	private static final String[] RELEVANCE_KEYS = {"none", "indirect", "main"};

	private final OpenAdminPayload data;
	private TextFieldWidget nameBox;
	private int relevance;
	private boolean targetable;
	private IdList pathList;
	private IdList dialogueList;
	private boolean deleteArmed;

	public AdminScreen(OpenAdminPayload data) {
		super(new TranslationTextComponent(data.npcEntityId() < 0 ? "npcdirector.gui.summon_title" : "npcdirector.gui.title"));
		this.data = data;
		this.relevance = data.relevance();
		this.targetable = data.targetable();
	}

	private boolean summonPanel() {
		return data.npcEntityId() < 0;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		if (summonPanel()) {
			this.addButton(new Button(centerX - 100, this.height / 2 - 20, 200, 20, new TranslationTextComponent("npcdirector.gui.summon"), b -> {
				NetworkingInit.sendToServer(new SummonNpcPayload());
				this.onClose();
			}));
			this.addButton(new Button(centerX - 100, this.height / 2 + 6, 200, 20, new TranslationTextComponent("npcdirector.gui.close"), b -> this.onClose()));
			return;
		}
		String previousName = nameBox == null ? data.name() : nameBox.getValue();
		int panel = Math.max(120, Math.min(300, this.width - 10));
		int half = (panel - 4) / 2;
		int third = (panel - 8) / 3;
		int left = centerX - panel / 2;
		nameBox = new TextFieldWidget(this.font, left, 34, panel, 18, new TranslationTextComponent("npcdirector.gui.name"));
		nameBox.setMaxLength(64);
		nameBox.setValue(previousName);
		this.addButton(nameBox);
		this.addButton(new Button(left, 58, half, 20, relevanceLabel(), b -> {
			relevance = (relevance + 1) % RELEVANCE_KEYS.length;
			b.setMessage(relevanceLabel());
		}));
		this.addButton(new Button(left + half + 4, 58, half, 20, targetableLabel(), b -> {
			targetable = !targetable;
			b.setMessage(targetableLabel());
		}));
		int listTop = 98;
		int listBottom = Math.max(listTop + 40, this.height - 56);
		String selectedPath = pathList == null ? data.pathId() : pathList.selectedId();
		String selectedDialogue = dialogueList == null ? data.dialogueId() : dialogueList.selectedId();
		pathList = new IdList(left, listTop, half, listBottom - listTop, data.paths(), selectedPath);
		dialogueList = new IdList(left + half + 4, listTop, half, listBottom - listTop, data.dialogues(), selectedDialogue);
		this.addButton(pathList);
		this.addButton(dialogueList);
		int buttonY = this.height - 28;
		this.addButton(new Button(left, buttonY, third, 20, new TranslationTextComponent("npcdirector.gui.apply"), b -> apply()));
		this.addButton(new Button(left + third + 4, buttonY, third, 20, new TranslationTextComponent("npcdirector.gui.close"), b -> this.onClose()));
		this.addButton(new Button(left + 2 * (third + 4), buttonY, third, 20,
				new TranslationTextComponent(deleteArmed ? "npcdirector.gui.delete_confirm" : "npcdirector.gui.delete"), b -> {
			if (!deleteArmed) {
				deleteArmed = true;
				b.setMessage(new TranslationTextComponent("npcdirector.gui.delete_confirm"));
				return;
			}
			NetworkingInit.sendToServer(new DeleteNpcPayload(data.npcEntityId()));
			this.onClose();
		}));
	}

	private ITextComponent relevanceLabel() {
		return new TranslationTextComponent("npcdirector.gui.relevance", new TranslationTextComponent("npcdirector.gui.relevance." + RELEVANCE_KEYS[relevance]));
	}

	private ITextComponent targetableLabel() {
		return new TranslationTextComponent("npcdirector.gui.targetable", new TranslationTextComponent(targetable ? "npcdirector.gui.yes" : "npcdirector.gui.no"));
	}

	private void apply() {
		NetworkingInit.sendToServer(new ApplyAdminPayload(data.npcEntityId(), nameBox.getValue(), relevance, targetable,
				pathList.selectedId(), dialogueList.selectedId()));
		this.onClose();
	}

	@Override
	public void tick() {
		super.tick();
		if (nameBox != null) {
			nameBox.tick();
		}
	}

	@Override
	public void render(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(poseStack);
		drawCenteredString(poseStack, this.font, this.title, this.width / 2, 12, 0xFFFFFF);
		if (!summonPanel()) {
			int panel = Math.max(120, Math.min(300, this.width - 10));
			int half = (panel - 4) / 2;
			int left = this.width / 2 - panel / 2;
			this.font.drawShadow(poseStack, new TranslationTextComponent("npcdirector.gui.paths"), left, 86, 0xAAAAAA);
			this.font.drawShadow(poseStack, new TranslationTextComponent("npcdirector.gui.dialogues"), left + half + 4, 86, 0xAAAAAA);
		}
		super.render(poseStack, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private class IdList extends Widget {

		private static final int ROW = 16;
		private final List<String> entries = new ArrayList<>();
		private int selected = -1;
		private int scroll;

		IdList(int x, int y, int width, int height, List<String> ids, String selectedId) {
			super(x, y, width, height, StringTextComponent.EMPTY);
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
			return Math.max(1, this.height / ROW);
		}

		private int maxScroll() {
			return Math.max(0, entries.size() - visibleRows());
		}

		@Override
		public void renderButton(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
			fill(matrix, this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1, 0xFF808080);
			fill(matrix, this.x, this.y, this.x + this.width, this.y + this.height, 0xFF000000);
			scroll = Math.max(0, Math.min(maxScroll(), scroll));
			int rows = visibleRows();
			for (int i = 0; i < rows && scroll + i < entries.size(); i++) {
				int index = scroll + i;
				String id = entries.get(index);
				String label = id.isEmpty() ? new TranslationTextComponent("npcdirector.gui.none").getString() : id;
				String shown = AdminScreen.this.font.plainSubstrByWidth(label, this.width - 10);
				int rowY = this.y + i * ROW;
				if (index == selected) {
					fill(matrix, this.x, rowY, this.x + this.width - 6, rowY + ROW, 0xFF404040);
				}
				AdminScreen.this.font.drawShadow(matrix, shown, this.x + 2, rowY + 4, index == selected ? 0xFFFF55 : 0xFFFFFF);
			}
			if (maxScroll() > 0) {
				int barHeight = Math.max(8, this.height * rows / entries.size());
				int barY = this.y + (this.height - barHeight) * scroll / maxScroll();
				fill(matrix, this.x + this.width - 5, barY, this.x + this.width - 1, barY + barHeight, 0xFFC0C0C0);
			}
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (button != 0 || mouseX < this.x || mouseX >= this.x + this.width || mouseY < this.y || mouseY >= this.y + this.height) {
				return false;
			}
			int index = scroll + (int) ((mouseY - this.y) / ROW);
			if (index >= 0 && index < entries.size()) {
				selected = index;
			}
			return true;
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
			if (mouseX < this.x || mouseX >= this.x + this.width || mouseY < this.y || mouseY >= this.y + this.height) {
				return false;
			}
			scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta)));
			return true;
		}
	}
}
