package com.tontoque28.npcdirector.network;

import net.minecraft.network.PacketBuffer;

public final class SavePathPayload {
	private final int npcEntityId;
	private final String name;
	private final PathData path;
	public SavePathPayload(int npcEntityId, String name, PathData path) {
		this.npcEntityId = npcEntityId;
		this.name = name;
		this.path = path;
	}
	public int npcEntityId() {
		return npcEntityId;
	}
	public String name() {
		return name;
	}
	public PathData path() {
		return path;
	}


	public static void encode(SavePathPayload payload, PacketBuffer buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.name(), 64);
		payload.path().write(buf);
	}

	public static SavePathPayload decode(PacketBuffer buf) {
		int id = buf.readVarInt();
		String name = buf.readUtf(64);
		return new SavePathPayload(id, name, PathData.read(buf));
	}
}
