package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public final class CloseDialoguePayload {

	public static void encode(CloseDialoguePayload payload, FriendlyByteBuf buf) {
	}

	public static CloseDialoguePayload decode(FriendlyByteBuf buf) {
		return new CloseDialoguePayload();
	}
}
