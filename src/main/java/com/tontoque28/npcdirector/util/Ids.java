package com.tontoque28.npcdirector.util;

import com.tontoque28.npcdirector.NpcDirector;
import net.minecraft.resources.Identifier;

public final class Ids {

	private Ids() {
	}

	public static Identifier parse(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		if (value.indexOf(Identifier.NAMESPACE_SEPARATOR) >= 0) {
			return Identifier.tryParse(value);
		}
		return Identifier.tryBuild(NpcDirector.MOD_ID, value);
	}
}
