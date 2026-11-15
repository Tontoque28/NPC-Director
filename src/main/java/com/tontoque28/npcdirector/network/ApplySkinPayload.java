package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public final class ApplySkinPayload {
	private final int npcEntityId;
	private final String skinName;
	public ApplySkinPayload(int npcEntityId, String skinName) {
		this.npcEntityId = npcEntityId;
		this.skinName = skinName;
	}
	public int npcEntityId() {
		return npcEntityId;
	}
	public String skinName() {
		return skinName;
	}


	public static final int NAME_MAX = 32;

	public static void encode(ApplySkinPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.skinName(), NAME_MAX);
	}

	public static ApplySkinPayload decode(FriendlyByteBuf buf) {
		return new ApplySkinPayload(buf.readVarInt(), buf.readUtf(NAME_MAX));
	}
}
