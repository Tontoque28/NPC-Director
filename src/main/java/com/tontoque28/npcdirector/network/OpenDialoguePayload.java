package com.tontoque28.npcdirector.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;

public record OpenDialoguePayload(String text, List<String> optionTexts, String npcName) {

	public static void encode(OpenDialoguePayload payload, FriendlyByteBuf buf) {
		buf.writeUtf(payload.text(), 32767);
		buf.writeVarInt(payload.optionTexts().size());
		for (String option : payload.optionTexts()) {
			buf.writeUtf(option, 32767);
		}
		buf.writeUtf(payload.npcName() == null ? "" : payload.npcName(), 256);
	}

	public static OpenDialoguePayload decode(FriendlyByteBuf buf) {
		String text = buf.readUtf(32767);
		int count = Math.min(buf.readVarInt(), 64);
		List<String> options = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			options.add(buf.readUtf(32767));
		}
		return new OpenDialoguePayload(text, options, buf.readUtf(256));
	}
}
