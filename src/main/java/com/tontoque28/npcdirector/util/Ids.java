package com.tontoque28.npcdirector.util;

import com.tontoque28.npcdirector.NpcDirector;
import net.minecraft.util.ResourceLocation;

public final class Ids {

	private Ids() {
	}

	public static ResourceLocation parse(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		if (value.indexOf(':') >= 0) {
			return ResourceLocation.tryParse(value);
		}
		return ResourceLocation.tryParse(NpcDirector.MOD_ID + ':' + value);
	}
}
