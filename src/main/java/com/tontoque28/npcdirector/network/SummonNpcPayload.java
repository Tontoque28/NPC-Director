package com.tontoque28.npcdirector.network;

import net.minecraft.network.PacketBuffer;

public final class SummonNpcPayload {
	public SummonNpcPayload() {
	}


	public static void encode(SummonNpcPayload payload, PacketBuffer buf) {
	}

	public static SummonNpcPayload decode(PacketBuffer buf) {
		return new SummonNpcPayload();
	}
}
