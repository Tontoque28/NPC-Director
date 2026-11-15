package com.tontoque28.npcdirector.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;

import com.tontoque28.npcdirector.network.AdminFields;
import com.tontoque28.npcdirector.network.ApplyAdminPayload;
import com.tontoque28.npcdirector.network.ApplySkinPayload;
import com.tontoque28.npcdirector.network.AssignPathPayload;
import com.tontoque28.npcdirector.network.DeleteNpcPayload;
import com.tontoque28.npcdirector.network.DialogueData;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.network.OutputSubscribePayload;
import com.tontoque28.npcdirector.network.PathData;
import com.tontoque28.npcdirector.network.SaveDialoguePayload;
import com.tontoque28.npcdirector.network.SavePathPayload;
import com.tontoque28.npcdirector.network.SummonNpcPayload;
import com.tontoque28.npcdirector.util.Validate;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public class AdminScreen extends Screen {

	private static final String[] RELEVANCE_KEYS = {"none", "indirect", "main"};
	private static final String[] ARM_KEYS = {"auto", "wide", "slim"};
	private static final String[] SLOT_KEYS = {"mainhand", "offhand", "head", "chest", "legs", "feet"};
	private static final String[] TAB_KEYS = {"general", "move", "talk", "look", "out"};
	private static final String[] SUB_KEYS = {"look", "gear", "sound"};
	private static final int RADIUS = 16;
	private static final int TAB_GENERAL = 0;
	private static final int TAB_MOVE = 1;
	private static final int TAB_TALK = 2;
	private static final int TAB_LOOK = 3;
	private static final int TAB_OUT = 4;
	private static final int SUB_LOOK = 0;
	private static final int SUB_GEAR = 1;
	private static final int ROW_H = 30;
	private static final int OUTPUT_MAX = 100;
	private static final int HEADER_Y = 6;
	private static final int HEADER_H = 22;
	private static final int TEXT_DARK = 0xFF202020;
	private static final List<String> OUTPUT = new ArrayList<>();

	private static final class Num {
		double value;
		final double min;
		final double max;
		final double step;
		final boolean integer;

		Num(double value, double min, double max, double step, boolean integer) {
			this.value = value;
			this.min = min;
			this.max = max;
			this.step = step;
			this.integer = integer;
		}

		void set(double raw) {
			double clamped = Math.max(min, Math.min(max, raw));
			value = integer ? Math.round(clamped) : Math.round(clamped * 100.0D) / 100.0D;
		}

		String text() {
			return integer ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.2f", value);
		}
	}

	private static final class OptState {
		String text = "";
		String commands = "";
		String next = "";
		String questId = "";
		String questState = "";
	}

	private static final class NodeState {
		String id;
		String text = "";
		final List<OptState> opts = new ArrayList<>();

		NodeState(String id) {
			this.id = id;
		}
	}

	private record Region(int x, int y, int w, int h, Component tip) {
	}

	private record Label(int x, int y, Component text, int color) {
	}

	private final OpenAdminPayload data;
	private int tab = TAB_GENERAL;
	private int subTab = SUB_LOOK;
	private int scroll = 0;
	private int maxRows = 0;
	private int visibleRows = 4;
	private boolean deleteArmed;
	private boolean subscribed;
	private boolean pendingRebuild;
	private boolean scrollEnd;
	private boolean importing;
	private String skinApplied;
	private int pathSel;
	private final List<String> pathChoices = new ArrayList<>();
	private Component status = Component.empty();

	private int leftX;
	private int mainW;
	private int bodyTop;
	private int bodyBottom;

	private String name;
	private boolean enabled;
	private int relevance;
	private boolean targetable;
	private String path;
	private String dialogue;
	private String skin;
	private final Num scale;
	private int arm;
	private boolean nameVisible;
	private int nameColor;
	private final String[] equip = new String[AdminFields.EQUIP_COUNT];
	private boolean lookAt;
	private boolean stationary;
	private boolean invulnerable;
	private boolean sitting;
	private final Num speed;
	private String greeting;
	private final Num greetRange;
	private String soundId;
	private final Num volume;
	private final Num pitch;

	private String pathName;
	private boolean pathLoop;
	private final List<String[]> points = new ArrayList<>();

	private String dialogueName;
	private final List<NodeState> nodes = new ArrayList<>();
	private int node = 0;
	private int editing = -1;

	private final List<Region> regions = new ArrayList<>();
	private final List<Label> labels = new ArrayList<>();
	private final List<int[]> emptyBoxes = new ArrayList<>();
	private final List<int[]> listBoxes = new ArrayList<>();
	private final List<Component> emptyTexts = new ArrayList<>();

	public AdminScreen(OpenAdminPayload data) {
		super(ClientLang.tr(data.npcEntityId() < 0 ? "npcdirector.gui.summon_title" : "npcdirector.gui.title"));
		this.data = data;
		AdminFields f = data.fields();
		this.name = data.name();
		this.enabled = f.enabled();
		this.relevance = Math.max(0, Math.min(2, data.relevance()));
		this.targetable = data.targetable();
		this.path = data.pathId();
		this.dialogue = data.dialogueId();
		this.skin = data.skinName();
		this.skinApplied = data.skinName();
		this.pathChoices.add("");
		this.pathChoices.addAll(data.paths());
		if (!data.pathId().isEmpty() && !this.pathChoices.contains(data.pathId())) {
			this.pathChoices.add(data.pathId());
		}
		this.pathSel = Math.max(0, this.pathChoices.indexOf(data.pathId()));
		this.scale = new Num(f.scale(), Validate.SCALE_MIN, Validate.SCALE_MAX, 0.1D, false);
		this.scale.set(f.scale());
		this.arm = Math.max(0, Math.min(2, f.armModel()));
		this.nameVisible = f.nameVisible();
		this.nameColor = Math.max(0, Math.min(15, f.nameColor()));
		for (int i = 0; i < AdminFields.EQUIP_COUNT; i++) {
			this.equip[i] = i < f.equip().size() ? f.equip().get(i) : "";
		}
		this.lookAt = f.lookAtPlayer();
		this.stationary = f.stationary();
		this.invulnerable = f.invulnerable();
		this.sitting = f.sitting();
		this.speed = new Num(f.patrolSpeed(), Validate.SPEED_MIN, Validate.SPEED_MAX, 0.1D, false);
		this.speed.set(f.patrolSpeed());
		this.greeting = f.greeting();
		this.greetRange = new Num(f.greetRange(), 1, 16, 1, true);
		this.greetRange.set(f.greetRange());
		this.soundId = f.soundId();
		this.volume = new Num(f.soundVolume(), 0.0D, 2.0D, 0.1D, false);
		this.volume.set(f.soundVolume());
		this.pitch = new Num(f.soundPitch(), 0.5D, 2.0D, 0.1D, false);
		this.pitch.set(f.soundPitch());

		this.pathName = data.pathId().startsWith("mundo:") ? data.pathId().substring(6) : "ruta_" + Math.max(0, data.npcEntityId());
		this.pathLoop = data.pathData().loop();
		for (int[] point : data.pathData().points()) {
			this.points.add(new String[] {String.valueOf(point[0]), String.valueOf(point[1]), String.valueOf(point[2])});
		}
		this.dialogueName = data.dialogueId().startsWith("mundo:") ? data.dialogueId().substring(6) : "dlg_" + Math.max(0, data.npcEntityId());
		for (DialogueData.Node source : data.dialogueData().nodes()) {
			NodeState state = new NodeState(source.id());
			state.text = source.text();
			for (DialogueData.Opt option : source.options()) {
				OptState opt = new OptState();
				opt.text = option.text();
				opt.commands = String.join("\n", option.commands());
				opt.next = option.next();
				opt.questId = option.questId();
				opt.questState = option.questState();
				state.opts.add(opt);
			}
			this.nodes.add(state);
		}
		if (this.nodes.isEmpty()) {
			this.nodes.add(new NodeState("inicio"));
		}
	}

	public static void pushOutput(List<String> lines) {
		OUTPUT.addAll(lines);
		while (OUTPUT.size() > OUTPUT_MAX) {
			OUTPUT.remove(0);
		}
	}

	private boolean summonPanel() {
		return data.npcEntityId() < 0;
	}

	private boolean wide() {
		return this.width >= 430;
	}

	private int contentTop() {
		return bodyTop + 8 + (tab == TAB_LOOK ? 24 : 0);
	}

	private boolean editorTab() {
		return tab != TAB_GENERAL;
	}

	private int cl() {
		return leftX + (editorTab() ? 16 : 6);
	}

	private int cw() {
		return mainW - (editorTab() ? 40 : 24);
	}

	private int rowY(int row) {
		return contentTop() + (row - scroll) * ROW_H;
	}

	private boolean visible(int row, int span) {
		return row >= scroll && row + Math.min(span, visibleRows) <= scroll + visibleRows;
	}

	private static Component t(String key) {
		return ClientLang.tr("npcdirector.gui." + key);
	}

	@Override
	protected void init() {
		regions.clear();
		labels.clear();
		emptyBoxes.clear();
		listBoxes.clear();
		emptyTexts.clear();
		int centerX = this.width / 2;
		if (summonPanel()) {
			this.addRenderableWidget(HoverButton.hoverBuilder(t("summon"), b -> {
				ClientNet.send(new SummonNpcPayload());
				this.onClose();
			}).bounds(centerX - 100, this.height / 2 - 20, 200, 20).tooltip(Tooltip.create(t("summon.tip"))).build());
			this.addRenderableWidget(HoverButton.hoverBuilder(t("close"), b -> this.onClose())
					.bounds(centerX - 100, this.height / 2 + 6, 200, 20).build());
			this.addRenderableWidget(HoverButton.hoverBuilder(ClientLang.tr("npcdirector.gui.lang.button", ClientLang.label()), b -> {
				ClientLang.cycle();
				this.rebuildWidgets();
			}).bounds(centerX - 100, this.height / 2 + 32, 200, 20).tooltip(Tooltip.create(ClientLang.tr("npcdirector.gui.lang.tip", ClientLang.label()))).build());
			return;
		}
		int sideW = wide() ? 128 : 0;
		mainW = Math.max(150, Math.min(300, this.width - sideW - (wide() ? 16 : 8)));
		int total = mainW + (wide() ? 4 + sideW : 0);
		leftX = Math.max(2, (this.width - total) / 2);
		bodyTop = HEADER_Y + HEADER_H + 2 + (wide() ? 0 : 22);
		bodyBottom = this.height - 30;
		visibleRows = Math.max(1, (bodyBottom - 6 - contentTop()) / ROW_H);

		buildHeader();
		buildTabs(sideW);
		maxRows = 0;
		switch (tab) {
			case TAB_GENERAL -> buildGeneral();
			case TAB_MOVE -> buildMove();
			case TAB_TALK -> buildTalk();
			case TAB_LOOK -> buildLookTab();
			default -> buildOutput();
		}
		int maxScroll = Math.max(0, maxRows - visibleRows);
		if (scrollEnd) {
			scrollEnd = false;
			if (scroll != maxScroll) {
				scroll = maxScroll;
				this.rebuildWidgets();
				return;
			}
		}
		int clamped = Math.max(0, Math.min(maxScroll, scroll));
		if (clamped != scroll) {
			scroll = clamped;
			this.rebuildWidgets();
			return;
		}
		if (maxScroll > 0 && tab != TAB_OUT) {
			int arrowX = leftX + mainW - 16;
			this.addRenderableWidget(HoverButton.hoverBuilder(Component.literal("^"), b -> scrollBy(-1))
					.bounds(arrowX, contentTop(), 12, 14).tooltip(Tooltip.create(t("scroll.tip"))).build());
			this.addRenderableWidget(HoverButton.hoverBuilder(Component.literal("v"), b -> scrollBy(1))
					.bounds(arrowX, contentTop() + 16, 12, 14).tooltip(Tooltip.create(t("scroll.tip"))).build());
		}
		int buttonY = this.height - 26;
		int footerW = Math.min(mainW, 260);
		int third = (footerW - 8) / 3;
		int footerX = leftX + (mainW - footerW) / 2;
		this.addRenderableWidget(HoverButton.hoverBuilder(t("apply"), b -> apply())
				.bounds(footerX, buttonY, third, 20).tooltip(Tooltip.create(t("apply.tip"))).build());
		this.addRenderableWidget(HoverButton.hoverBuilder(t("close"), b -> this.onClose())
				.bounds(footerX + third + 4, buttonY, third, 20).tooltip(Tooltip.create(t("close.tip"))).build());
		this.addRenderableWidget(HoverButton.hoverBuilder(t(deleteArmed ? "delete_confirm" : "delete"), b -> {
			if (!deleteArmed) {
				deleteArmed = true;
				b.setMessage(t("delete_confirm"));
				return;
			}
			ClientNet.send(new DeleteNpcPayload(data.npcEntityId()));
			this.onClose();
		}).bounds(footerX + 2 * (third + 4), buttonY, third, 20).tooltip(Tooltip.create(t("delete.tip"))).build());
	}

	private void buildHeader() {
		int stateW = Math.min(96, mainW / 3);
		int langW = mainW < 200 ? 30 : 38;
		int nameW = mainW - stateW - langW - 16;
		EditBox box = new EditBox(this.font, leftX + 4, HEADER_Y + 2, nameW, 18, t("name"));
		box.setMaxLength(64);
		box.setHint(t("name.hint"));
		box.setValue(name);
		box.setTooltip(Tooltip.create(t("name.tip")));
		box.setResponder(text -> name = text);
		this.addRenderableWidget(box);
		this.addRenderableWidget(HoverButton.hoverBuilder(Component.literal(ClientLang.shortLabel()), b -> {
			ClientLang.cycle();
			this.rebuildWidgets();
		}).bounds(leftX + mainW - stateW - langW - 8, HEADER_Y + 1, langW, 20).tooltip(Tooltip.create(ClientLang.tr("npcdirector.gui.lang.tip", ClientLang.label()))).build());
		Component state = ClientLang.tr("npcdirector.gui.state", Component.literal(enabled ? "ON" : "OFF").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
		HoverButton stateButton = HoverButton.hoverBuilder(state, b -> {
			enabled = !enabled;
			this.rebuildWidgets();
		}).bounds(leftX + mainW - stateW - 4, HEADER_Y + 1, stateW, 20).tooltip(Tooltip.create(t("state.tip"))).build();
		stateButton.tint = enabled ? 0x5000C000 : 0x50C00000;
		this.addRenderableWidget(stateButton);
	}

	private void buildTabs(int sideW) {
		for (int i = 1; i < TAB_KEYS.length; i++) {
			int index = i;
			Component label = t("tab." + TAB_KEYS[i] + (wide() ? "" : ".short"));
			HoverButton button;
			if (wide()) {
				button = HoverButton.hoverBuilder(label, b -> selectTab(index))
						.bounds(leftX + mainW + 4, bodyTop + (i - 1) * 24, sideW, 22).tooltip(Tooltip.create(t("tab." + TAB_KEYS[i] + ".tip"))).build();
			} else {
				int each = (mainW - 6) / 4;
				button = HoverButton.hoverBuilder(label, b -> selectTab(index))
						.bounds(leftX + (i - 1) * (each + 2), HEADER_Y + HEADER_H + 2, each, 18).tooltip(Tooltip.create(t("tab." + TAB_KEYS[i] + ".tip"))).build();
			}
			button.selected = tab == i;
			this.addRenderableWidget(button);
		}
	}

	private void selectTab(int index) {
		boolean wasOut = tab == TAB_OUT;
		tab = index;
		scroll = 0;
		editing = -1;
		status = Component.empty();
		boolean nowOut = tab == TAB_OUT;
		if (nowOut && !subscribed) {
			subscribed = true;
			ClientNet.send(new OutputSubscribePayload(true));
		} else if (wasOut && !nowOut && subscribed) {
			subscribed = false;
			ClientNet.send(new OutputSubscribePayload(false));
		}
		this.rebuildWidgets();
	}

	@Override
	public void removed() {
		if (subscribed) {
			subscribed = false;
			ClientNet.send(new OutputSubscribePayload(false));
		}
		super.removed();
	}

	private void scrollBy(int delta) {
		int maxScroll = Math.max(0, maxRows - visibleRows);
		int next = Math.max(0, Math.min(maxScroll, scroll + delta));
		if (next != scroll) {
			scroll = next;
			this.rebuildWidgets();
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (!summonPanel() && super.mouseScrolled(mouseX, mouseY, delta)) {
			return true;
		}
		if (!summonPanel() && maxRows > visibleRows) {
			scrollBy(delta > 0 ? -1 : 1);
			return true;
		}
		return false;
	}

	private void label(int x, int y, Component text, Component tip, int width) {
		labels.add(new Label(x, y, text, TEXT_DARK));
		regions.add(new Region(x, y, Math.min(width, this.font.width(text)), 9, tip));
	}

	private EditBox editBox(int x, int y, int width, String value, int maxLength, Component hint, Component tip, Consumer<String> sink) {
		EditBox box = new EditBox(this.font, x, y, width, 18, hint);
		box.setMaxLength(maxLength);
		box.setHint(hint);
		box.setValue(value);
		box.setTooltip(Tooltip.create(tip));
		box.setResponder(sink);
		this.addRenderableWidget(box);
		return box;
	}

	private Button button(int x, int y, int width, Component text, Component tip, Runnable action) {
		Button button = HoverButton.hoverBuilder(text, b -> action.run()).bounds(x, y, width, 20).tooltip(Tooltip.create(tip)).build();
		this.addRenderableWidget(button);
		return button;
	}

	private MultiLineEditBox bigBox(int x, int y, int width, int height, String value, int limit, Component hint, Component tip, Consumer<String> sink) {
		MultiLineEditBox box = new MultiLineEditBox(this.font, x, y, width, height, hint, hint);
		box.setCharacterLimit(limit);
		box.setValue(value);
		box.setValueListener(sink);
		box.setTooltip(Tooltip.create(tip));
		this.addRenderableWidget(box);
		return box;
	}

	private Component yesNo(String key, boolean value) {
		return ClientLang.tr("npcdirector.gui." + key, t(value ? "yes" : "no"));
	}

	private void rowSpanCheck(int row, int span) {
		maxRows = Math.max(maxRows, row + span);
	}

	private void toggleRow(int row, String keyA, boolean valueA, Runnable flipA, String keyB, boolean valueB, Runnable flipB) {
		rowSpanCheck(row, 1);
		if (!visible(row, 1)) {
			return;
		}
		int half = (cw() - 4) / 2;
		int y = rowY(row) + 8;
		button(cl(), y, half, yesNo(keyA, valueA), t(keyA + ".tip"), () -> {
			flipA.run();
			this.rebuildWidgets();
		});
		if (keyB != null) {
			button(cl() + half + 4, y, half, yesNo(keyB, valueB), t(keyB + ".tip"), () -> {
				flipB.run();
				this.rebuildWidgets();
			});
		}
	}

	private void stepperRow(int row, String key, Num num) {
		rowSpanCheck(row, 1);
		if (!visible(row, 1)) {
			return;
		}
		int y = rowY(row);
		int x = cl();
		int w = cw();
		label(x, y, t(key), t(key + ".tip"), w);
		button(x, y + 10, 20, Component.literal("-"), t(key + ".tip"), () -> {
			num.set(num.value - num.step);
			this.rebuildWidgets();
		});
		editBox(x + 22, y + 10, w - 44, num.text(), 6, t(key + ".hint"), t(key + ".tip"), text -> {
			try {
				num.set(Double.parseDouble(text.trim().replace(',', '.')));
			} catch (NumberFormatException exception) {
				return;
			}
		});
		button(x + w - 20, y + 10, 20, Component.literal("+"), t(key + ".tip"), () -> {
			num.set(num.value + num.step);
			this.rebuildWidgets();
		});
	}

	private void textRow(int row, String key, String value, int maxLength, Consumer<String> sink) {
		rowSpanCheck(row, 1);
		if (!visible(row, 1)) {
			return;
		}
		int y = rowY(row);
		label(cl(), y, t(key), t(key + ".tip"), cw());
		editBox(cl(), y + 10, cw(), value, maxLength, t(key + ".hint"), t(key + ".tip"), sink);
	}

	private void buildGeneral() {
		int half = (cw() - 4) / 2;
		rowSpanCheck(0, 1);
		if (visible(0, 1)) {
			int y = rowY(0);
			button(cl(), y + 4, half, relevanceLabel(), t("relevance.tip"), () -> {
				relevance = (relevance + 1) % RELEVANCE_KEYS.length;
				this.rebuildWidgets();
			});
			button(cl() + half + 4, y + 4, half, yesNo("targetable", targetable), t("targetable.tip"), () -> {
				targetable = !targetable;
				this.rebuildWidgets();
			});
		}
		int span = Math.max(2, visibleRows - 1);
		rowSpanCheck(1, span);
		if (!visible(1, span)) {
			return;
		}
		int top = rowY(1);
		int listHeight = Math.min(span, visibleRows - 1) * ROW_H - 14;
		buildList(cl(), top, cw(), listHeight, "dialogues", data.dialogues());
	}

	private void buildList(int x, int top, int width, int height, String key, List<String> ids) {
		Component title = ClientLang.tr("npcdirector.gui." + key + ".title", ids.size());
		labels.add(new Label(x, top, title, TEXT_DARK));
		regions.add(new Region(x, top, width, height + 11, t(key + ".tip")));
		int listTop = top + 11;
		if (ids.isEmpty()) {
			emptyBoxes.add(new int[] {x, listTop, width, height});
			emptyTexts.add(t(key + ".empty"));
			return;
		}
		String selected = dialogue;
		listBoxes.add(new int[] {x, listTop, width, height});
		this.addRenderableWidget(new IdList(x, listTop, listTop + height, width, ids, selected));
	}

	private String pointName(int index) {
		return index == 0 ? ClientLang.tr("npcdirector.gui.point.start").getString() : ClientLang.tr("npcdirector.gui.point.n", index).getString();
	}

	private String pathChoice() {
		return pathChoices.get(Math.max(0, Math.min(pathSel, pathChoices.size() - 1)));
	}

	private void stepPath(int delta) {
		pathSel = (pathSel + delta + pathChoices.size()) % pathChoices.size();
		this.rebuildWidgets();
	}

	private void assignPath(boolean radius) {
		String chosen = pathChoice();
		ClientNet.send(new AssignPathPayload(data.npcEntityId(), chosen, radius));
		path = chosen;
		status = t("saving");
		this.rebuildWidgets();
	}

	private void buildMove() {
		rowSpanCheck(0, 1);
		if (visible(0, 1)) {
			Component current = ClientLang.tr("npcdirector.gui.path.current", path.isEmpty() ? t("none").getString() : path);
			label(cl(), rowY(0) + 4, current, t("path.current.tip"), cw());
		}
		rowSpanCheck(1, 1);
		if (visible(1, 1)) {
			int y = rowY(1) + 4;
			int arrow = 22;
			int mid = Math.max(20, cw() - 2 * arrow - 8);
			button(cl(), y, arrow, Component.literal("<"), t("path.prev.tip"), () -> stepPath(-1));
			String shown = pathChoice().isEmpty() ? t("none").getString() : pathChoice();
			button(cl() + arrow + 4, y, mid, Component.literal(this.font.plainSubstrByWidth(shown, mid - 8)), t("path.select.tip"), () -> stepPath(1));
			button(cl() + arrow + 8 + mid, y, arrow, Component.literal(">"), t("path.next.tip"), () -> stepPath(1));
		}
		rowSpanCheck(2, 1);
		if (visible(2, 1)) {
			int half = (cw() - 4) / 2;
			int y = rowY(2) + 4;
			button(cl(), y, half, t("path.assign_this"), t("path.assign_this.tip"), () -> assignPath(false));
			button(cl() + half + 4, y, half, ClientLang.tr("npcdirector.gui.path.assign_radius", RADIUS), ClientLang.tr("npcdirector.gui.path.assign_radius.tip", RADIUS), () -> assignPath(true));
		}
		textRow(3, "path_name", pathName, 32, text -> pathName = text.toLowerCase(Locale.ROOT));
		rowSpanCheck(4, 1);
		if (visible(4, 1)) {
			button(cl(), rowY(4) + 6, cw(), yesNo("loop", pathLoop), t("loop.tip"), () -> {
				pathLoop = !pathLoop;
				this.rebuildWidgets();
			});
		}
		int fixW = Math.min(24, cw() / 6);
		int boxW = Math.max(18, (cw() - fixW - 8) / 3);
		String[] axes = {"X", "Y", "Z"};
		int row = 5;
		if (points.isEmpty()) {
			rowSpanCheck(row, 1);
			if (visible(row, 1)) {
				label(cl(), rowY(row) + 8, t("point.none"), t("point.tip"), cw());
			}
			row++;
		}
		for (int i = 0; i < points.size(); i++) {
			int index = i;
			rowSpanCheck(row, 1);
			if (visible(row, 1)) {
				int y = rowY(row);
				String[] texts = points.get(i);
				label(cl(), y, Component.literal(pointName(i)), t("point.tip"), cw());
				for (int k = 0; k < 3; k++) {
					int axis = k;
					CoordBox box = new CoordBox(this.font, cl() + k * (boxW + 2), y + 10, boxW, 18, Component.literal(axes[k]), () -> {
						if (splitPoint(texts)) {
							pendingRebuild = true;
						}
					});
					box.setMaxLength(160);
					box.setHint(Component.literal(axes[k]));
					box.setValue(texts[k]);
					box.setTooltip(Tooltip.create(t("point.coord.tip")));
					box.setResponder(value -> {
						String before = texts[axis];
						texts[axis] = value;
						if (value.length() - before.length() >= 3 && splitPoint(texts)) {
							pendingRebuild = true;
						}
					});
					this.addRenderableWidget(box);
				}
				button(cl() + 3 * (boxW + 2), y + 10, cw() - 3 * (boxW + 2), Component.literal("x"), t("point.remove.tip"), () -> {
					points.remove(index);
					this.rebuildWidgets();
				});
			}
			row++;
		}
		rowSpanCheck(row, 1);
		if (visible(row, 1)) {
			button(cl(), rowY(row) + 4, cw(), t("point.add_here"), t("point.add_here.tip"), () -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player != null && points.size() < PathData.MAX_POINTS) {
					points.add(new String[] {String.valueOf((int) Math.floor(minecraft.player.getX())), String.valueOf((int) Math.floor(minecraft.player.getY())), String.valueOf((int) Math.floor(minecraft.player.getZ()))});
					scrollEnd = true;
				}
				this.rebuildWidgets();
			});
		}
		row++;
		rowSpanCheck(row, 1);
		if (visible(row, 1)) {
			button(cl(), rowY(row) + 4, cw(), t("path.save"), t("path.save.tip"), this::savePath);
		}
		row++;
		rowSpanCheck(row, 1);
		if (visible(row, 1)) {
			label(cl(), rowY(row) + 8, t("behavior.title"), t("behavior.tip"), cw());
		}
		row++;
		toggleRow(row, "look_at", lookAt, () -> lookAt = !lookAt, "still", stationary, () -> stationary = !stationary);
		toggleRow(row + 1, "invulnerable", invulnerable, () -> invulnerable = !invulnerable, "sit", sitting, () -> sitting = !sitting);
		stepperRow(row + 2, "speed", speed);
	}

	private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");

	private static int[] parseTriple(String raw) {
		String value = raw == null ? "" : raw.trim();
		if (value.isEmpty() || value.matches("-?\\d+")) {
			return null;
		}
		if (value.chars().anyMatch(Character::isWhitespace)) {
			value = value.replaceAll("(?<=[0-9]),(?=[0-9])", ".");
		}
		Matcher matcher = NUMBER.matcher(value);
		int[] numbers = new int[3];
		int count = 0;
		while (count < 3 && matcher.find()) {
			double parsed;
			try {
				parsed = Double.parseDouble(matcher.group());
			} catch (NumberFormatException exception) {
				return null;
			}
			if (Math.abs(parsed) > 1.0E9D) {
				return null;
			}
			numbers[count++] = (int) Math.floor(parsed);
		}
		return count == 3 ? numbers : null;
	}

	private static boolean splitPoint(String[] texts) {
		for (int k = 0; k < 3; k++) {
			int[] numbers = parseTriple(texts[k]);
			if (numbers != null) {
				for (int j = 0; j < 3; j++) {
					texts[j] = String.valueOf(numbers[j]);
				}
				return true;
			}
		}
		return false;
	}

	private static final class CoordBox extends EditBox {

		private final Runnable onBlur;

		CoordBox(net.minecraft.client.gui.Font font, int x, int y, int width, int height, Component message, Runnable onBlur) {
			super(font, x, y, width, height, message);
			this.onBlur = onBlur;
		}

		@Override
		public void setFocused(boolean focused) {
			boolean was = this.isFocused();
			super.setFocused(focused);
			if (was && !focused) {
				onBlur.run();
			}
		}
	}

	private static String[] emptyPoint() {
		return new String[] {"", "", ""};
	}

	private static boolean blankPoint(String[] texts) {
		return texts[0].trim().isEmpty() && texts[1].trim().isEmpty() && texts[2].trim().isEmpty();
	}

	private static Integer parseCoord(String text, int min, int max) {
		try {
			double parsed = Double.parseDouble(text.trim().replace(',', '.'));
			if (Double.isNaN(parsed) || Math.abs(parsed) > 1.0E9D) {
				return null;
			}
			int value = (int) Math.floor(parsed);
			return value < min || value > max ? null : value;
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private void savePath() {
		for (String[] texts : points) {
			splitPoint(texts);
		}
		List<int[]> set = new ArrayList<>();
		for (String[] texts : points) {
			if (blankPoint(texts)) {
				continue;
			}
			Integer x = parseCoord(texts[0], -30000000, 30000000);
			Integer y = parseCoord(texts[1], -2048, 4096);
			Integer z = parseCoord(texts[2], -30000000, 30000000);
			if (x == null || y == null || z == null) {
				status = t("point.invalid");
				return;
			}
			set.add(new int[] {x, y, z});
		}
		String clean = pathName == null ? "" : pathName.trim();
		if (set.isEmpty()) {
			status = t("path.empty");
			return;
		}
		if (!com.tontoque28.npcdirector.world.WorldContent.validName(clean)) {
			status = t("name.invalid");
			return;
		}
		ClientNet.send(new SavePathPayload(data.npcEntityId(), clean, new PathData(pathLoop, set)));
		path = "mundo:" + clean;
		if (!pathChoices.contains(path)) {
			pathChoices.add(path);
		}
		pathSel = pathChoices.indexOf(path);
		status = t("saving");
		this.rebuildWidgets();
	}

	private NodeState current() {
		node = Math.max(0, Math.min(node, nodes.size() - 1));
		return nodes.get(node);
	}

	private void buildTalk() {
		if (editing >= 0 && editing < current().opts.size()) {
			buildButtonEditor();
			return;
		}
		editing = -1;
		textRow(0, "dialogue_name", dialogueName, 32, text -> dialogueName = text.toLowerCase(Locale.ROOT));
		rowSpanCheck(1, 1);
		if (visible(1, 1)) {
			int y = rowY(1) + 4;
			int arrow = 22;
			int add = Math.min(70, cw() / 3);
			button(cl(), y, arrow, Component.literal("<"), t("node.prev.tip"), () -> {
				node = (node + nodes.size() - 1) % nodes.size();
				this.rebuildWidgets();
			});
			Component nodeLabel = ClientLang.tr("npcdirector.gui.node", current().id);
			int mid = cw() - 2 * arrow - add - 12;
			button(cl() + arrow + 4, y, mid, nodeLabel, t("node.tip"), () -> {
			});
			button(cl() + arrow + 8 + mid, y, arrow, Component.literal(">"), t("node.next.tip"), () -> {
				node = (node + 1) % nodes.size();
				this.rebuildWidgets();
			});
			button(cl() + cw() - add, y, add, t("node.add"), t("node.add.tip"), () -> {
				if (nodes.size() < 16) {
					int n = nodes.size() + 1;
					String id = "nodo" + n;
					while (nodeExists(id)) {
						n++;
						id = "nodo" + n;
					}
					nodes.add(new NodeState(id));
					node = nodes.size() - 1;
				}
				this.rebuildWidgets();
			});
		}
		int textSpan = Math.min(3, visibleRows);
		rowSpanCheck(2, 3);
		if (visible(2, 3)) {
			int y = rowY(2);
			label(cl(), y, t("npc_says"), t("npc_says.tip"), cw());
			bigBox(cl(), y + 10, cw(), textSpan * ROW_H - 12, current().text, 8000, t("npc_says.hint"), t("npc_says.tip"), text -> current().text = text);
		}
		int cols = cw() >= 150 ? 3 : 2;
		int buttonW = (cw() - (cols - 1) * 4) / cols;
		List<OptState> opts = current().opts;
		int gridRows = (opts.size() + cols - 1) / cols;
		rowSpanCheck(5, 1);
		if (visible(5, 1)) {
			label(cl(), rowY(5) + 8, ClientLang.tr("npcdirector.gui.buttons.title", opts.size()), t("buttons.tip"), cw());
		}
		for (int g = 0; g < gridRows; g++) {
			int row = 6 + g;
			rowSpanCheck(row, 1);
			if (!visible(row, 1)) {
				continue;
			}
			for (int c = 0; c < cols; c++) {
				int index = g * cols + c;
				if (index >= opts.size()) {
					break;
				}
				button(cl() + c * (buttonW + 4), rowY(row) + 4, buttonW, ClientLang.tr("npcdirector.gui.button_n", index + 1), t("button_n.tip"), () -> {
					editing = index;
					scroll = 0;
					this.rebuildWidgets();
				});
			}
		}
		int row = 6 + gridRows;
		rowSpanCheck(row, 1);
		if (visible(row, 1)) {
			int half = (cw() - 4) / 2;
			button(cl(), rowY(row) + 4, half, t("button.add"), t("button.add.tip"), () -> {
				if (current().opts.size() < 64) {
					current().opts.add(new OptState());
					editing = current().opts.size() - 1;
					scroll = 0;
				}
				this.rebuildWidgets();
			});
			button(cl() + half + 4, rowY(row) + 4, half, t("dialogue.save"), t("dialogue.save.tip"), this::saveDialogue);
		}
	}

	private boolean nodeExists(String id) {
		for (NodeState state : nodes) {
			if (state.id.equals(id)) {
				return true;
			}
		}
		return false;
	}

	private void buildButtonEditor() {
		OptState opt = current().opts.get(editing);
		int index = editing;
		rowSpanCheck(0, 1);
		if (visible(0, 1)) {
			int y = rowY(0);
			label(cl(), y, ClientLang.tr("npcdirector.gui.button_text", index + 1), t("button_text.tip"), cw());
			editBox(cl(), y + 10, cw(), opt.text, 512, t("button_text.hint"), t("button_text.tip"), text -> opt.text = text);
		}
		int span = Math.min(3, visibleRows);
		rowSpanCheck(1, 3);
		if (visible(1, 3)) {
			int y = rowY(1);
			label(cl(), y, t("commands"), t("commands.tip"), cw());
			bigBox(cl(), y + 10, cw(), span * ROW_H - 12, opt.commands, 4096, t("commands.hint"), t("commands.tip"), text -> opt.commands = text);
		}
		textRow(4, "next_node", opt.next, 32, text -> opt.next = text.trim());
		rowSpanCheck(5, 1);
		if (visible(5, 1)) {
			int y = rowY(5);
			int half = (cw() - 4) / 2;
			label(cl(), y, t("quest_id"), t("quest_id.tip"), half);
			editBox(cl(), y + 10, half, opt.questId, 64, t("quest_id.hint"), t("quest_id.tip"), text -> opt.questId = text.trim());
			label(cl() + half + 4, y, t("quest_state"), t("quest_state.tip"), half);
			editBox(cl() + half + 4, y + 10, half, opt.questState, 64, t("quest_state.hint"), t("quest_state.tip"), text -> opt.questState = text.trim());
		}
		rowSpanCheck(6, 1);
		if (visible(6, 1)) {
			int half = (cw() - 4) / 2;
			int okW = Math.min(half, 60);
			button(cl(), rowY(6) + 4, okW, t("ok"), t("ok.tip"), () -> {
				editing = -1;
				scroll = 0;
				this.rebuildWidgets();
			});
			button(cl() + okW + 4, rowY(6) + 4, Math.min(half, 90), t("button.remove"), t("button.remove.tip"), () -> {
				current().opts.remove(index);
				editing = -1;
				scroll = 0;
				this.rebuildWidgets();
			});
		}
	}

	private void saveDialogue() {
		String clean = dialogueName == null ? "" : dialogueName.trim();
		if (!com.tontoque28.npcdirector.world.WorldContent.validName(clean)) {
			status = t("name.invalid");
			return;
		}
		List<DialogueData.Node> out = new ArrayList<>();
		for (NodeState state : nodes) {
			List<DialogueData.Opt> options = new ArrayList<>();
			for (OptState opt : state.opts) {
				List<String> commands = new ArrayList<>();
				for (String line : opt.commands.split("\n")) {
					if (!line.trim().isEmpty()) {
						commands.add(line.trim());
					}
				}
				options.add(new DialogueData.Opt(opt.text, commands, opt.next, opt.questId, opt.questState));
			}
			out.add(new DialogueData.Node(state.id, state.text, options));
		}
		DialogueData payloadData = new DialogueData(out);
		if (payloadData.estimateBytes() > DialogueData.SAFE_BYTES) {
			status = t("dialogue.too_big");
			return;
		}
		ClientNet.send(new SaveDialoguePayload(data.npcEntityId(), clean, payloadData));
		dialogue = "mundo:" + clean;
		status = t("saving");
		this.rebuildWidgets();
	}

	private void buildLookTab() {
		int each = (mainW - 24 - (SUB_KEYS.length - 1) * 2) / SUB_KEYS.length;
		for (int i = 0; i < SUB_KEYS.length; i++) {
			int index = i;
			Component label = t("sub." + SUB_KEYS[i] + (each < 52 ? ".short" : ""));
			HoverButton button = HoverButton.hoverBuilder(label, b -> {
				subTab = index;
				scroll = 0;
				this.rebuildWidgets();
			}).bounds(leftX + 12 + i * (each + 2), bodyTop + 8, each, 18).tooltip(Tooltip.create(t("sub." + SUB_KEYS[i] + ".tip"))).build();
			button.selected = subTab == i;
			this.addRenderableWidget(button);
		}
		switch (subTab) {
			case SUB_LOOK -> buildLook();
			case SUB_GEAR -> buildGear();
			default -> buildSound();
		}
	}

	private void buildLook() {
		rowSpanCheck(0, 1);
		if (visible(0, 1)) {
			int y = rowY(0);
			label(cl(), y, t("skin"), t("skin.tip"), cw());
			int applyWidth = Math.min(cw() / 2, Math.max(60, this.font.width(t("skin_apply")) + 12));
			editBox(cl(), y + 10, cw() - applyWidth - 4, skin, 16, t("skin.hint"), t("skin.tip"), text -> skin = text);
			button(cl() + cw() - applyWidth, y + 9, applyWidth, t("skin_apply"), t("skin_apply.tip"), () -> {
				skinApplied = skin.trim();
				ClientNet.send(new ApplySkinPayload(data.npcEntityId(), skinApplied));
			});
		}
		stepperRow(1, "scale", scale);
		int half = (cw() - 4) / 2;
		rowSpanCheck(2, 1);
		if (visible(2, 1)) {
			int y = rowY(2) + 8;
			button(cl(), y, half, ClientLang.tr("npcdirector.gui.arms", t("arms." + ARM_KEYS[arm])), t("arms.tip"), () -> {
				arm = (arm + 1) % ARM_KEYS.length;
				this.rebuildWidgets();
			});
			button(cl() + half + 4, y, half, yesNo("show_name", nameVisible), t("show_name.tip"), () -> {
				nameVisible = !nameVisible;
				this.rebuildWidgets();
			});
		}
		rowSpanCheck(3, 1);
		if (visible(3, 1)) {
			int y = rowY(3) + 8;
			Component colorName = ClientLang.tr("npcdirector.color." + nameColor).withStyle(Validate.format(nameColor));
			button(cl(), y, cw(), ClientLang.tr("npcdirector.gui.name_color", colorName), t("name_color.tip"), () -> {
				nameColor = (nameColor + 1) % 16;
				this.rebuildWidgets();
			});
		}
		rowSpanCheck(4, 1);
		if (visible(4, 1)) {
			Button importButton = button(cl(), rowY(4) + 8, cw(), t("skin_import"), t("skin_import.tip"), this::importSkin);
			importButton.active = !importing;
		}
	}

	private void importSkin() {
		if (importing) {
			return;
		}
		SkinImport.pickAsync(() -> {
			importing = true;
			status = t("skin_import.opening");
			this.rebuildWidgets();
		}, chosen -> {
			importing = false;
			if (Minecraft.getInstance().screen != this) {
				return;
			}
			if (chosen == null) {
				status = Component.empty();
			} else {
				SkinImport.Outcome outcome = SkinImport.send(data.npcEntityId(), chosen);
				status = outcome.message();
				if (outcome.sent()) {
					skin = "";
					skinApplied = "";
				}
			}
			this.rebuildWidgets();
		});
	}

	private void buildGear() {
		Component useHeld = t("use_held");
		int buttonWidth = Math.min(cw() / 2, this.font.width(useHeld) + 12);
		for (int i = 0; i < AdminFields.EQUIP_COUNT; i++) {
			int index = i;
			rowSpanCheck(i, 1);
			if (!visible(i, 1)) {
				continue;
			}
			int y = rowY(i);
			label(cl(), y, t("slot." + SLOT_KEYS[i]), t("equip.tip"), cw());
			EditBox box = editBox(cl(), y + 10, cw() - buttonWidth - 4, equip[i], 64, t("equip.hint"), t("equip.tip"), text -> equip[index] = text);
			box.setTextColor(itemValid(equip[i]) ? 0xFFE0E0E0 : 0xFFFF5555);
			box.setResponder(text -> {
				equip[index] = text;
				box.setTextColor(itemValid(text) ? 0xFFE0E0E0 : 0xFFFF5555);
			});
			button(cl() + cw() - buttonWidth, y + 9, buttonWidth, useHeld, t("use_held.tip"), () -> {
				equip[index] = heldItemId();
				this.rebuildWidgets();
			});
		}
	}

	private static boolean itemValid(String id) {
		return Validate.isClear(id) || Validate.item(id) != null;
	}

	private static String heldItemId() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return "";
		}
		ItemStack held = minecraft.player.getMainHandItem();
		if (held.isEmpty()) {
			return "";
		}
		return BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
	}

	private void buildSound() {
		textRow(0, "greeting", greeting, 256, text -> greeting = text);
		stepperRow(1, "greet_range", greetRange);
		rowSpanCheck(2, 1);
		if (visible(2, 1)) {
			int y = rowY(2);
			label(cl(), y, t("sound_id"), t("sound_id.tip"), cw());
			EditBox box = editBox(cl(), y + 10, cw(), soundId, 128, t("sound_id.hint"), t("sound_id.tip"), text -> soundId = text);
			box.setTextColor(soundValid(soundId) ? 0xFFE0E0E0 : 0xFFFF5555);
			box.setResponder(text -> {
				soundId = text;
				box.setTextColor(soundValid(text) ? 0xFFE0E0E0 : 0xFFFF5555);
			});
		}
		stepperRow(3, "volume", volume);
		stepperRow(4, "pitch", pitch);
	}

	private static boolean soundValid(String id) {
		return Validate.isClear(id) || Validate.sound(id) != null;
	}

	private void buildOutput() {
		int clearW = Math.min(80, cw() / 3);
		button(leftX + mainW - 16 - clearW, bodyTop + 8, clearW, t("out.clear"), t("out.clear.tip"), () -> OUTPUT.clear());
	}

	private Component relevanceLabel() {
		return ClientLang.tr("npcdirector.gui.relevance", ClientLang.tr("npcdirector.gui.relevance." + RELEVANCE_KEYS[relevance]));
	}

	private void apply() {
		String wanted = skin == null ? "" : skin.trim();
		if (!wanted.equals(skinApplied)) {
			skinApplied = wanted;
			ClientNet.send(new ApplySkinPayload(data.npcEntityId(), wanted));
		}
		List<String> items = new ArrayList<>();
		for (String value : equip) {
			items.add(value == null ? "" : value.trim());
		}
		AdminFields fields = new AdminFields((float) scale.value, arm, nameVisible, nameColor, items, lookAt, stationary, invulnerable,
				(float) speed.value, sitting, greeting, (int) greetRange.value, soundId == null ? "" : soundId.trim(), (float) volume.value, (float) pitch.value, enabled);
		ClientNet.send(new ApplyAdminPayload(data.npcEntityId(), name, relevance, targetable, path, dialogue, fields));
		this.onClose();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
		if (summonPanel()) {
			guiGraphics.drawCenteredString(this.font, t("summon_title"), this.width / 2, 8, 0xFFFFFFFF);
			super.render(guiGraphics, mouseX, mouseY, partialTick);
			return;
		}
		drawFrame(guiGraphics);
		for (int[] box : listBoxes) {
			guiGraphics.fill(box[0] - 1, box[1] - 1, box[0] + box[2] + 1, box[1] + box[3] + 1, 0xFF404040);
			guiGraphics.fill(box[0], box[1], box[0] + box[2], box[1] + box[3], 0xFF101010);
		}
		for (int i = 0; i < emptyBoxes.size(); i++) {
			int[] box = emptyBoxes.get(i);
			guiGraphics.fill(box[0], box[1], box[0] + box[2], box[1] + box[3], 0xFFB8B8B8);
			List<FormattedCharSequence> lines = this.font.split(emptyTexts.get(i), Math.max(20, box[2] - 6));
			int y = box[1] + 3;
			for (FormattedCharSequence line : lines) {
				if (y + 9 > box[1] + box[3]) {
					break;
				}
				guiGraphics.drawString(this.font, line, box[0] + 3, y, 0xFF404040, false);
				y += 10;
			}
		}
		for (Label label : labels) {
			guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(label.text().getString(), cw() + 8), label.x(), label.y(), label.color(), false);
		}
		if (tab == TAB_LOOK && subTab == SUB_GEAR) {
			drawStatusMarks(guiGraphics);
		}
		if (tab == TAB_OUT) {
			drawOutput(guiGraphics);
		}
		if (!status.getString().isEmpty() && tab != TAB_OUT) {
			List<FormattedCharSequence> statusLines = this.font.split(status, mainW - 20);
			int shown = Math.min(2, statusLines.size());
			for (int i = 0; i < shown; i++) {
				guiGraphics.drawString(this.font, statusLines.get(i), leftX + 10, bodyBottom - 11 - (shown - 1 - i) * 10, 0xFF7A1F1F, false);
			}
		}
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		for (Region region : regions) {
			if (mouseX >= region.x() && mouseX < region.x() + region.w() && mouseY >= region.y() && mouseY < region.y() + region.h()) {
				guiGraphics.renderTooltip(this.font, this.font.split(region.tip(), 200), mouseX, mouseY);
				break;
			}
		}
	}

	private void drawFrame(GuiGraphics g) {
		g.fill(leftX - 1, HEADER_Y - 1, leftX + mainW + 1, bodyBottom + 1, 0xFF303030);
		g.fill(leftX, HEADER_Y, leftX + mainW, HEADER_Y + HEADER_H, 0xFF8B8B8B);
		g.fill(leftX, bodyTop - 2, leftX + mainW, bodyBottom, 0xFFD6D6D6);
		if (editorTab()) {
			int x0 = leftX + 8;
			int x1 = leftX + mainW - 8;
			int y0 = bodyTop + 2;
			int y1 = bodyBottom - 4;
			g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF404040);
			g.fill(x0, y0, x1, y1, tab == TAB_OUT ? 0xFF000000 : 0xFFEDEDED);
		}
		g.fill(leftX, bodyBottom, leftX + mainW, bodyBottom + 1, 0xFF303030);
	}

	private void drawOutput(GuiGraphics g) {
		int x = leftX + 14;
		g.drawString(this.font, t("commands"), x, bodyTop + 12, 0xFF55FF55, false);
		int top = bodyTop + 30;
		int bottom = bodyBottom - 8;
		int lineHeight = 10;
		int capacity = Math.max(1, (bottom - top) / lineHeight);
		if (!data.dev() && OUTPUT.isEmpty()) {
			List<FormattedCharSequence> lines = this.font.split(t("out.dev_off"), mainW - 36);
			int y = top;
			for (FormattedCharSequence line : lines) {
				g.drawString(this.font, line, x, y, 0xFFFFAA00, false);
				y += lineHeight;
			}
			return;
		}
		int start = Math.max(0, OUTPUT.size() - capacity);
		int y = top;
		for (int i = start; i < OUTPUT.size(); i++) {
			String line = OUTPUT.get(i);
			int color = line.contains(" FALLO ") || line.contains("rechazad") ? 0xFFFF5555 : 0xFFE0E0E0;
			g.drawString(this.font, this.font.plainSubstrByWidth(line, mainW - 36), x, y, color, false);
			y += lineHeight;
		}
	}

	private void drawStatusMarks(GuiGraphics guiGraphics) {
		for (int i = 0; i < AdminFields.EQUIP_COUNT; i++) {
			if (!visible(i, 1) || Validate.isClear(equip[i])) {
				continue;
			}
			boolean ok = Validate.item(equip[i]) != null;
			Component mark = t(ok ? "item_ok" : "item_missing");
			int x = cl() + cw() - this.font.width(mark);
			guiGraphics.drawString(this.font, mark, x, rowY(i), ok ? 0xFF006600 : 0xFFAA0000, false);
		}
	}

	public void showServerMessage(Component message) {
		if (message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable && ClientLang.mode() != 0) {
			status = ClientLang.tr(translatable.getKey(), translatable.getArgs());
		} else {
			status = message;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (pendingRebuild) {
			pendingRebuild = false;
			this.rebuildWidgets();
		}
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
			public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
				Component label = id.isEmpty() ? ClientLang.tr("npcdirector.gui.none") : Component.literal(id);
				String shown = AdminScreen.this.font.plainSubstrByWidth(label.getString(), width - 4);
				guiGraphics.drawString(AdminScreen.this.font, shown, left + 2, top + 3, IdList.this.getSelected() == this ? 0xFFFFFF55 : hovered ? 0xFF8CE0FF : 0xFFFFFFFF);
			}

			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				IdList.this.setSelected(this);
				AdminScreen.this.dialogue = id;
				return true;
			}

			@Override
			public Component getNarration() {
				return id.isEmpty() ? ClientLang.tr("npcdirector.gui.none") : Component.literal(id);
			}
		}

		@Override
		public void updateNarration(NarrationElementOutput output) {
			super.updateNarration(output);
		}
	}
}
