package com.tontoque28.npcdirector.util;

import java.util.regex.Pattern;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class Validate {

	public static final float SCALE_MIN = 0.5F;
	public static final float SCALE_MAX = 2.0F;
	public static final float SPEED_MIN = 0.5F;
	public static final float SPEED_MAX = 2.0F;
	public static final int GREETING_MAX = 256;
	public static final int ID_MAX = 128;
	private static final Pattern SKIN_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
	private static final Pattern NODE_ID = Pattern.compile("[a-z0-9_]{1,32}");
	private static final Pattern QUEST_TOKEN = Pattern.compile("[A-Za-z0-9_.:-]{1,64}");

	private Validate() {
	}

	public static boolean validSkinName(String name) {
		return name != null && SKIN_NAME.matcher(name).matches();
	}

	public static float clampFloat(float value, float min, float max, float fallback) {
		if (Float.isNaN(value) || Float.isInfinite(value)) {
			return fallback;
		}
		return Math.max(min, Math.min(max, value));
	}

	public static boolean finite(float value) {
		return !Float.isNaN(value) && !Float.isInfinite(value);
	}

	public static String cleanText(String raw, int max) {
		if (raw == null) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < raw.length() && builder.length() < max; i++) {
			char c = raw.charAt(i);
			if (c >= 32 && c != 127 && c != '§') {
				builder.append(c);
			}
		}
		return builder.toString().trim();
	}

	public static boolean nodeId(String id) {
		return id != null && NODE_ID.matcher(id).matches();
	}

	public static boolean questToken(String value) {
		return value != null && QUEST_TOKEN.matcher(value).matches();
	}

	public static String cleanMultiline(String raw, int max) {
		if (raw == null) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < raw.length() && builder.length() < max; i++) {
			char c = raw.charAt(i);
			if (c == '\n' || (c >= 32 && c != 127)) {
				builder.append(c);
			}
		}
		return builder.toString().trim();
	}

	public static net.minecraft.ChatFormatting format(int id) {
		return net.minecraft.ChatFormatting.values()[Math.max(0, Math.min(15, id))];
	}

	public static boolean isClear(String id) {
		if (id == null) {
			return true;
		}
		String value = id.trim();
		return value.isEmpty() || value.equalsIgnoreCase("none") || value.equalsIgnoreCase("minecraft:air") || value.equalsIgnoreCase("air");
	}

	public static Item item(String id) {
		if (id == null || id.length() > ID_MAX) {
			return null;
		}
		Identifier location = Identifier.tryParse(id.trim().toLowerCase());
		if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) {
			return null;
		}
		Item item = BuiltInRegistries.ITEM.getValue(location);
		return item == Items.AIR ? null : item;
	}

	public static Identifier sound(String id) {
		if (id == null || id.length() > ID_MAX) {
			return null;
		}
		Identifier location = Identifier.tryParse(id.trim().toLowerCase());
		if (location == null || !BuiltInRegistries.SOUND_EVENT.containsKey(location)) {
			return null;
		}
		return location;
	}

	public static SoundEvent soundEvent(String id) {
		Identifier location = sound(id);
		return location == null ? null : BuiltInRegistries.SOUND_EVENT.getValue(location);
	}
}
