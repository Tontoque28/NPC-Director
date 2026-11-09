package com.tontoque28.npcdirector.client;

import java.util.ArrayList;
import java.util.List;

import com.tontoque28.npcdirector.network.ApplyAdminPayload;
import com.tontoque28.npcdirector.network.DeleteNpcPayload;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.network.SummonNpcPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AdminScreen extends Screen {

	private static final String[] RELEVANCE_KEYS = {"none", "indirect", "main"};

	private final OpenAdminPayload data;
	private EditBox nameBox;
	private int relevance;
	private boolean targetable;
	private Button relevanceButton;
	private Button targetableButton;
	private IdList pathList;
	private IdList dialogueList;
	private boolean deleteArmed;

	public AdminScreen(OpenAdminPayload data) {
		super(Component.translatable(data.npcEntityId() < 0 ? "npcdirector.gui.summon_title" : "npcdirector.gui.title"));
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
			this.addRenderableWidget(Button.builder(Component.translatable("npcdirector.gui.summon"), b -> {
				NetworkingInit.sendToServer(new SummonNpcPayload());
				this.onClose();
			}).bounds(centerX - 100, this.height / 2 - 20, 200, 20).build());
			this.addRenderableWidget(Button.builder(Component.translatable("npcdirector.gui.close"), b -> this.onClose())
					.bounds(centerX - 100, this.height / 2 + 6, 200, 20).build());
			return;
		}
		String previousName = nameBox == null ? data.name() : nameBox.getValue();
		int panel = Math.max(120, Math.min(300, this.width - 10));
		int half = (panel - 4) / 2;
		int third = (panel - 8) / 3;
		int left = centerX - panel / 2;
		nameBox = new EditBox(this.font, left, 34, panel, 18, Component.translatable("npcdirector.gui.name"));
		nameBox.setMaxLength(64);
		nameBox.setValue(previousName);
		this.addRenderableWidget(nameBox);
		relevanceButton = Button.builder(relevanceLabel(), b -> {
			relevance = (relevance + 1) % RELEVANCE_KEYS.length;
			b.setMessage(relevanceLabel());
		}).bounds(left, 58, half, 20).build();
		this.addRenderableWidget(relevanceButton);
		targetableButton = Button.builder(targetableLabel(), b -> {
			targetable = !targetable;
			b.setMessage(targetableLabel());
		}).bounds(left + half + 4, 58, half, 20).build();
		this.addRenderableWidget(targetableButton);
		int listTop = 98;
		int listBottom = Math.max(listTop + 40, this.height - 56);
		String selectedPath = pathList == null ? data.pathId() : pathList.selectedId();
		String selectedDialogue = dialogueList == null ? data.dialogueId() : dialogueList.selectedId();
		pathList = new IdList(left, listTop, listBottom, half, data.paths(), selectedPath);
		dialogueList = new IdList(left + half + 4, listTop, listBottom, half, data.dialogues(), selectedDialogue);
		this.addRenderableWidget(pathList);
		this.addRenderableWidget(dialogueList);
		int buttonY = this.height - 28;
		this.addRenderableWidget(Button.builder(Component.translatable("npcdirector.gui.apply"), b -> apply())
				.bounds(left, buttonY, third, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("npcdirector.gui.close"), b -> this.onClose())
				.bounds(left + third + 4, buttonY, third, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable(deleteArmed ? "npcdirector.gui.delete_confirm" : "npcdirector.gui.delete"), b -> {
			if (!deleteArmed) {
				deleteArmed = true;
				b.setMessage(Component.translatable("npcdirector.gui.delete_confirm"));
				return;
			}
			NetworkingInit.sendToServer(new DeleteNpcPayload(data.npcEntityId()));
			this.onClose();
		}).bounds(left + 2 * (third + 4), buttonY, third, 20).build());
	}

	private Component relevanceLabel() {
		return Component.translatable("npcdirector.gui.relevance", Component.translatable("npcdirector.gui.relevance." + RELEVANCE_KEYS[relevance]));
	}

	private Component targetableLabel() {
		return Component.translatable("npcdirector.gui.targetable", Component.translatable(targetable ? "npcdirector.gui.yes" : "npcdirector.gui.no"));
	}

	private void apply() {
		NetworkingInit.sendToServer(new ApplyAdminPayload(data.npcEntityId(), nameBox.getValue(), relevance, targetable,
				pathList.selectedId(), dialogueList.selectedId()));
		this.onClose();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
		if (!summonPanel()) {
			int panel = Math.max(120, Math.min(300, this.width - 10));
			int half = (panel - 4) / 2;
			int left = this.width / 2 - panel / 2;
			guiGraphics.drawString(this.font, Component.translatable("npcdirector.gui.paths"), left, 86, 0xAAAAAA);
			guiGraphics.drawString(this.font, Component.translatable("npcdirector.gui.dialogues"), left + half + 4, 86, 0xAAAAAA);
		}
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private class IdList extends ObjectSelectionList<IdList.IdEntry> {

		IdList(int left, int top, int bottom, int width, List<String> ids, String selected) {
			super(AdminScreen.this.minecraft, width, bottom - top, top, 16);
			this.setX(left);
			List<String> all = new ArrayList<>();
			all.add("");
			all.addAll(ids);
			for (String id : all) {
				IdEntry entry = new IdEntry(id);
				this.addEntry(entry);
				if (id.equals(selected)) {
					this.setSelected(entry);
				}
			}
			if (this.getSelected() == null && !selected.isEmpty()) {
				IdEntry extra = new IdEntry(selected);
				this.addEntry(extra);
				this.setSelected(extra);
			}
			if (this.getSelected() == null && this.children().size() > 0) {
				this.setSelected(this.children().get(0));
			}
		}

		String selectedId() {
			IdEntry entry = this.getSelected();
			return entry == null ? "" : entry.id;
		}

		@Override
		protected int getScrollbarPosition() {
			return this.getX() + this.getWidth() - 6;
		}

		@Override
		public int getRowWidth() {
			return this.width - 10;
		}

		class IdEntry extends ObjectSelectionList.Entry<IdEntry> {

			final String id;

			IdEntry(String id) {
				this.id = id;
			}

			@Override
			public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
				Component label = id.isEmpty() ? Component.translatable("npcdirector.gui.none") : Component.literal(id);
				String shown = AdminScreen.this.font.plainSubstrByWidth(label.getString(), width - 4);
				guiGraphics.drawString(AdminScreen.this.font, shown, left + 2, top + 3, IdList.this.getSelected() == this ? 0xFFFF55 : 0xFFFFFF);
			}

			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				IdList.this.setSelected(this);
				return true;
			}

			@Override
			public Component getNarration() {
				return id.isEmpty() ? Component.translatable("npcdirector.gui.none") : Component.literal(id);
			}
		}
	}
}
