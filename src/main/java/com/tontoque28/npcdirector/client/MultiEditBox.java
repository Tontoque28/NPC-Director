package com.tontoque28.npcdirector.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.util.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;

public class MultiEditBox extends Widget implements Tipped {

	private static final char NEWLINE = (char) 10;

	private final FontRenderer font;
	private final ITextComponent hint;
	private ITextComponent tip;
	private String value = "";
	private int limit = 32767;
	private int cursor;
	private int scrollLine;
	private int blink;
	private boolean focusedBox;
	private Consumer<String> listener = text -> {
	};
	private final List<int[]> lines = new ArrayList<>();
	private boolean dirty = true;
	private int layoutWidth = -1;

	public MultiEditBox(FontRenderer font, int x, int y, int width, int height, ITextComponent hint) {
		super(x, y, width, height, hint);
		this.font = font;
		this.hint = hint;
	}

	public void setCharacterLimit(int limit) {
		this.limit = limit;
	}

	public void setValueListener(Consumer<String> listener) {
		this.listener = listener;
	}

	public void setTooltip(Tooltip tooltip) {
		this.tip = tooltip.text();
	}

	@Override
	public ITextComponent tip() {
		return tip;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String text) {
		this.value = text.length() > limit ? text.substring(0, limit) : text;
		this.cursor = this.value.length();
		this.dirty = true;
	}

	public void tick() {
		blink++;
	}

	private int innerWidth() {
		return this.width - 10;
	}

	private int visibleLines() {
		return Math.max(1, (this.height - 8) / 10);
	}

	private void layout() {
		if (!dirty && layoutWidth == innerWidth()) {
			return;
		}
		dirty = false;
		layoutWidth = innerWidth();
		lines.clear();
		int pos = 0;
		int n = value.length();
		while (true) {
			int nl = value.indexOf(NEWLINE, pos);
			int end = nl < 0 ? n : nl;
			int start = pos;
			if (start == end) {
				lines.add(new int[] {start, end});
			}
			while (start < end) {
				String rest = value.substring(start, end);
				String fit = font.plainSubstrByWidth(rest, layoutWidth);
				int take = Math.max(1, fit.length());
				if (take < rest.length()) {
					int space = rest.lastIndexOf(' ', take - 1);
					if (space > 0) {
						take = space + 1;
					}
				}
				lines.add(new int[] {start, start + take});
				start += take;
			}
			if (nl < 0) {
				break;
			}
			pos = nl + 1;
			if (pos == n) {
				lines.add(new int[] {n, n});
				break;
			}
		}
	}

	private int lineOf(int index) {
		layout();
		int found = 0;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i)[0] <= index) {
				found = i;
			} else {
				break;
			}
		}
		return found;
	}

	private void insert(String text) {
		StringBuilder clean = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == NEWLINE || SharedConstants.isAllowedChatCharacter(c)) {
				clean.append(c);
			}
		}
		int room = limit - value.length();
		if (room <= 0 || clean.length() == 0) {
			return;
		}
		String add = clean.length() > room ? clean.substring(0, room) : clean.toString();
		value = value.substring(0, cursor) + add + value.substring(cursor);
		cursor += add.length();
		changed();
	}

	private void changed() {
		dirty = true;
		listener.accept(value);
		ensureVisible();
	}

	private void ensureVisible() {
		int line = lineOf(cursor);
		if (line < scrollLine) {
			scrollLine = line;
		} else if (line >= scrollLine + visibleLines()) {
			scrollLine = line - visibleLines() + 1;
		}
	}

	@Override
	public boolean charTyped(char c, int modifiers) {
		if (!this.active || !this.visible || !focusedBox) {
			return false;
		}
		insert(String.valueOf(c));
		return true;
	}

	@Override
	public boolean changeFocus(boolean forward) {
		if (!this.active || !this.visible) {
			return false;
		}
		focusedBox = !focusedBox;
		return focusedBox;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!this.active || !this.visible || !focusedBox) {
			return false;
		}
		if (Screen.isPaste(keyCode)) {
			insert(Minecraft.getInstance().keyboardHandler.getClipboard());
			return true;
		}
		layout();
		switch (keyCode) {
			case 257:
			case 335:
				insert(String.valueOf(NEWLINE));
				return true;
			case 259:
				if (cursor > 0) {
					value = value.substring(0, cursor - 1) + value.substring(cursor);
					cursor--;
					changed();
				}
				return true;
			case 261:
				if (cursor < value.length()) {
					value = value.substring(0, cursor) + value.substring(cursor + 1);
					changed();
				}
				return true;
			case 263:
				cursor = Math.max(0, cursor - 1);
				ensureVisible();
				return true;
			case 262:
				cursor = Math.min(value.length(), cursor + 1);
				ensureVisible();
				return true;
			case 268:
				cursor = lines.get(lineOf(cursor))[0];
				ensureVisible();
				return true;
			case 269:
				cursor = lines.get(lineOf(cursor))[1];
				ensureVisible();
				return true;
			case 265:
			case 264: {
				int line = lineOf(cursor);
				int target = keyCode == 265 ? line - 1 : line + 1;
				if (target >= 0 && target < lines.size()) {
					int[] from = lines.get(line);
					int width = font.width(value.substring(from[0], Math.max(from[0], Math.min(cursor, from[1]))));
					int[] to = lines.get(target);
					String text = value.substring(to[0], to[1]);
					cursor = to[0] + font.plainSubstrByWidth(text, width).length();
					ensureVisible();
				}
				return true;
			}
			default:
				return false;
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean inside = this.active && this.visible && mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
		focusedBox = inside;
		if (inside && button == 0) {
			layout();
			int row = scrollLine + (int) ((mouseY - this.y - 4) / 10);
			row = Math.max(0, Math.min(lines.size() - 1, row));
			int[] line = lines.get(row);
			String text = value.substring(line[0], line[1]);
			cursor = line[0] + font.plainSubstrByWidth(text, (int) mouseX - this.x - 5).length();
			ensureVisible();
		}
		return inside;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (!this.visible || mouseX < this.x || mouseX >= this.x + this.width || mouseY < this.y || mouseY >= this.y + this.height) {
			return false;
		}
		layout();
		int max = Math.max(0, lines.size() - visibleLines());
		scrollLine = Math.max(0, Math.min(max, scrollLine - (int) Math.signum(delta)));
		return true;
	}

	@Override
	public void renderButton(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
		layout();
		int border = focusedBox ? 0xFFFFFFFF : 0xFFA0A0A0;
		AbstractGui.fill(poseStack, this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1, border);
		AbstractGui.fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, 0xFF000000);
		if (value.isEmpty() && !focusedBox) {
			font.draw(poseStack, font.plainSubstrByWidth(hint.getString(), innerWidth()), (float) (this.x + 5), (float) (this.y + 4), 0xFF808080);
			return;
		}
		int visible = visibleLines();
		int cursorLine = lineOf(cursor);
		for (int i = 0; i < visible && scrollLine + i < lines.size(); i++) {
			int[] line = lines.get(scrollLine + i);
			String text = value.substring(line[0], line[1]);
			int y = this.y + 4 + i * 10;
			font.drawShadow(poseStack, text.replace(String.valueOf(NEWLINE), ""), (float) (this.x + 5), (float) y, 0xFFFFFFFF);
			if (focusedBox && (blink / 6) % 2 == 0 && scrollLine + i == cursorLine) {
				int[] cl = lines.get(cursorLine);
				int cx = this.x + 5 + font.width(value.substring(cl[0], Math.max(cl[0], Math.min(cursor, cl[1]))));
				AbstractGui.fill(poseStack, cx, y - 1, cx + 1, y + 9, 0xFFFFFFFF);
			}
		}
		if (lines.size() > visible) {
			int trackH = this.height - 4;
			int barH = Math.max(6, trackH * visible / lines.size());
			int barY = this.y + 2 + (trackH - barH) * scrollLine / Math.max(1, lines.size() - visible);
			AbstractGui.fill(poseStack, this.x + this.width - 3, barY, this.x + this.width - 1, barY + barH, 0xFFA0A0A0);
		}
	}
}
