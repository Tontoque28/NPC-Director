package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public final class ChooseDialoguePayload {

	private final int optionIndex;

	public ChooseDialoguePayload(int optionIndex) {
		this.optionIndex = optionIndex;
	}

	public int optionIndex() {
		return optionIndex;
	}

	public static void encode(ChooseDialoguePayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.optionIndex());
	}

	public static ChooseDialoguePayload decode(FriendlyByteBuf buf) {
		return new ChooseDialoguePayload(buf.readVarInt());
	}
}
