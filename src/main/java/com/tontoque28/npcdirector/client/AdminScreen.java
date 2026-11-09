package com.tontoque28.npcdirector.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tontoque28.npcdirector.network.ApplyAdminPayload;
import com.tontoque28.npcdirector.network.DeleteNpcPayload;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.network.SummonNpcPayload;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public class AdminScreen extends Screen {

	private static final String[] RELEVANCE_KEYS = {"none", "indirect", "main"};

	private final OpenAdminPayload data;
	private EditBox nameBox;
	private int relevance;
	private boolean targetable;
	private IdList pathList;
	private IdList dialogueList;
	private boolean deleteArmed;

	public AdminScreen(OpenAdminPayload data) {
		super(new TranslatableComponent(data.npcEntityId() < 0 ? "npcdirector.gui.summon_title" : "npcdirector.gui.title"));
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
			this.addButton(new Button(centerX - 100, this.height / 2 - 20, 200, 20, new TranslatableComponent("npcdirector.gui.summon"), b -> {
				ClientNet.send(new SummonNpcPayload());
				this.onClose();
			}));
			this.addButton(new Button(centerX - 100, this.height / 2 + 6, 200, 20, new TranslatableComponent("npcdirector.gui.close"), b -> this.onClose()));
			return;
		}
		String previousName = nameBox == null ? data.name() : nameBox.getValue();
		int panel = Math.max(120, Math.min(300, this.width - 10));
		int half = (panel - 4) / 2;
		int third = (panel - 8) / 3;
		int left = centerX - panel / 2;
		nameBox = new EditBox(this.font, left, 34, panel, 18, new TranslatableComponent("npcdirector.gui.name"));
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
		pathList = new IdList(left, listTop, listBottom, half, data.paths(), selectedPath);
		dialogueList = new IdList(left + half + 4, listTop, listBottom, half, data.dialogues(), selectedDialogue);
		this.children.add(pathList);
		this.children.add(dialogueList);
		int buttonY = this.height - 28;
		this.addButton(new Button(left, buttonY, third, 20, new TranslatableComponent("npcdirector.gui.apply"), b -> apply()));
		this.addButton(new Button(left + third + 4, buttonY, third, 20, new TranslatableComponent("npcdirector.gui.close"), b -> this.onClose()));
		this.addButton(new Button(left + 2 * (third + 4), buttonY, third, 20,
				new TranslatableComponent(deleteArmed ? "npcdirector.gui.delete_confirm" : "npcdirector.gui.delete"), b -> {
			if (!deleteArmed) {
				deleteArmed = true;
				b.setMessage(new TranslatableComponent("npcdirector.gui.delete_confirm"));
				return;
			}
			ClientNet.send(new DeleteNpcPayload(data.npcEntityId()));
			this.onClose();
		}));
	}

	private Component relevanceLabel() {
		return new TranslatableComponent("npcdirector.gui.relevance", new TranslatableComponent("npcdirector.gui.relevance." + RELEVANCE_KEYS[relevance]));
	}

	private Component targetableLabel() {
		return new TranslatableComponent("npcdirector.gui.targetable", new TranslatableComponent(targetable ? "npcdirector.gui.yes" : "npcdirector.gui.no"));
	}

	private void apply() {
		ClientNet.send(new ApplyAdminPayload(data.npcEntityId(), nameBox.getValue(), relevance, targetable,
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
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(poseStack);
		drawCenteredString(poseStack, this.font, this.title, this.width / 2, 12, 0xFFFFFF);
		if (!summonPanel()) {
			int panel = Math.max(120, Math.min(300, this.width - 10));
			int half = (panel - 4) / 2;
			int left = this.width / 2 - panel / 2;
			drawString(poseStack, this.font, new TranslatableComponent("npcdirector.gui.paths"), left, 86, 0xAAAAAA);
			drawString(poseStack, this.font, new TranslatableComponent("npcdirector.gui.dialogues"), left + half + 4, 86, 0xAAAAAA);
			pathList.render(poseStack, mouseX, mouseY, partialTick);
			dialogueList.render(poseStack, mouseX, mouseY, partialTick);
		}
		super.render(poseStack, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private class IdList extends ObjectSelectionList<IdList.IdEntry> {

		IdList(int left, int top, int bottom, int width, List<String> ids, String selected) {
			super(AdminScreen.this.minecraft, width, AdminScreen.this.height, top, bottom, 16);
			this.setLeftPos(left);
			this.setRenderBackground(false);
			this.setRenderTopAndBottom(false);
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
			return this.x1 - 6;
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
			public void render(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
				String label = id.isEmpty() ? new TranslatableComponent("npcdirector.gui.none").getString() : id;
				String shown = AdminScreen.this.font.plainSubstrByWidth(label, width - 4);
				AdminScreen.this.font.drawShadow(poseStack, shown, (float) (left + 2), (float) (top + 3), IdList.this.getSelected() == this ? 0xFFFF55 : 0xFFFFFF);
			}

			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				IdList.this.setSelected(this);
				return true;
			}
		}
	}
}
