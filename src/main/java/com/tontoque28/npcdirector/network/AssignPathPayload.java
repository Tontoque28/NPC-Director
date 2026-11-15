package com.tontoque28.npcdirector.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class AssignPathPayload implements IMessage {

	private int npcEntityId;
	private String pathId = "";
	private boolean radius;

	public AssignPathPayload() {
	}

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

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeVarInt(buf, npcEntityId, 5);
		ByteBufUtils.writeUTF8String(buf, pathId.length() > 128 ? pathId.substring(0, 128) : pathId);
		buf.writeBoolean(radius);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		npcEntityId = ByteBufUtils.readVarInt(buf, 5);
		String id = ByteBufUtils.readUTF8String(buf);
		pathId = id.length() > 128 ? id.substring(0, 128) : id;
		radius = buf.readBoolean();
	}
}
