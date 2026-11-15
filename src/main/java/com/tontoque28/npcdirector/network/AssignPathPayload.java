package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;

public final class AssignPathPayload {
	private final int npcEntityId;
	private final String pathId;
	private final boolean radius;

	public AssignPathPayload(int npcEntityId, String pathId, boolean radius) {
		this.npcEntityId = npcEntityId;
		this.pathId = pathId;
		this.radius = radius;
	}

	public int npcEntityId() {
		return npcEntityId;
	}

	public String pathId() {
		return pathId;
	}

	public boolean radius() {
		return radius;
	}

	public static void encode(AssignPathPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.pathId(), 128);
		buf.writeBoolean(payload.radius());
	}

	public static AssignPathPayload decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		String path = buf.readUtf(128);
		return new AssignPathPayload(id, path, buf.readBoolean());
	}
}
