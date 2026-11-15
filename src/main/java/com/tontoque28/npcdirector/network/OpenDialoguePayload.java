package com.tontoque28.npcdirector.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.PacketBuffer;

public final class OpenDialoguePayload {
	private final String text;
	private final List<String> optionTexts;
	private final String npcName;
	public OpenDialoguePayload(String text, List<String> optionTexts, String npcName) {
		this.text = text;
		this.optionTexts = optionTexts;
		this.npcName = npcName;
	}
	public String npcName() {
		return npcName;
	}
	public String text() {
		return text;
	}
	public List<String> optionTexts() {
		return optionTexts;
	}


	public static void encode(OpenDialoguePayload payload, PacketBuffer buf) {
		buf.writeUtf(payload.text());
		buf.writeVarInt(payload.optionTexts().size());
		for (String option : payload.optionTexts()) {
			buf.writeUtf(option);
		}
		buf.writeUtf(payload.npcName() == null ? "" : payload.npcName(), 256);
	}

	public static OpenDialoguePayload decode(PacketBuffer buf) {
		String text = buf.readUtf();
		int count = Math.min(buf.readVarInt(), 64);
		List<String> options = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			options.add(buf.readUtf());
		}
		return new OpenDialoguePayload(text, options, buf.readUtf(256));
	}
}
