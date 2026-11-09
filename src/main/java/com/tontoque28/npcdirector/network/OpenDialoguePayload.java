package com.tontoque28.npcdirector.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public final class OpenDialoguePayload {

	private final String text;
	private final List<String> optionTexts;

	public OpenDialoguePayload(String text, List<String> optionTexts) {
		this.text = text;
		this.optionTexts = optionTexts;
	}

	public String text() {
		return text;
	}

	public List<String> optionTexts() {
		return optionTexts;
	}

	public static void encode(OpenDialoguePayload payload, FriendlyByteBuf buf) {
		buf.writeUtf(payload.text());
		buf.writeVarInt(payload.optionTexts().size());
		for (String option : payload.optionTexts()) {
			buf.writeUtf(option);
		}
	}

	public static OpenDialoguePayload decode(FriendlyByteBuf buf) {
		String text = buf.readUtf();
		int count = buf.readVarInt();
		List<String> options = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			options.add(buf.readUtf());
		}
		return new OpenDialoguePayload(text, options);
	}
}
