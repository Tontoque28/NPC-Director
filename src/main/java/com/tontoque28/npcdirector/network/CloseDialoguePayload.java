package com.tontoque28.npcdirector.network;

import net.minecraft.network.PacketBuffer;

public class CloseDialoguePayload {

	public static void encode(CloseDialoguePayload payload, PacketBuffer buf) {
	}

	public static CloseDialoguePayload decode(PacketBuffer buf) {
		return new CloseDialoguePayload();
	}
}
