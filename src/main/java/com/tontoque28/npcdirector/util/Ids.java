package com.tontoque28.npcdirector.util;

import com.tontoque28.npcdirector.NpcDirector;
import net.minecraft.util.ResourceLocation;

import java.util.regex.Pattern;

public final class Ids {

	private static final Pattern VALID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

	private Ids() {
	}

	public static ResourceLocation parse(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		String full = value.indexOf(':') >= 0 ? value : NpcDirector.MOD_ID + ':' + value;
		if (!VALID.matcher(full).matches()) {
			return null;
		}
		return new ResourceLocation(full);
	}
}
