package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public record ChooseDialoguePayload(int optionIndex) {

	public static void encode(ChooseDialoguePayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.optionIndex());
	}

	public static ChooseDialoguePayload decode(FriendlyByteBuf buf) {
		return new ChooseDialoguePayload(buf.readVarInt());
	}
}
