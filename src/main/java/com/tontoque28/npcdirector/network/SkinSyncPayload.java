package com.tontoque28.npcdirector.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class SkinSyncPayload implements IMessage {

	private int npcEntityId;
	private String hash = "";
	private byte[] png = new byte[0];

	public SkinSyncPayload() {
	}

	public SkinSyncPayload(int npcEntityId, String hash, byte[] png) {
		this.npcEntityId = npcEntityId;
		this.hash = hash;
		this.png = png;
	}

	public int npcEntityId() {
		return npcEntityId;
	}

	public String hash() {
		return hash;
	}

	public byte[] png() {
		return png;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeVarInt(buf, npcEntityId, 5);
		ByteBufUtils.writeUTF8String(buf, hash);
		ByteBufUtils.writeVarInt(buf, png.length, 5);
		buf.writeBytes(png);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		npcEntityId = ByteBufUtils.readVarInt(buf, 5);
		hash = ByteBufUtils.readUTF8String(buf);
		int length = ByteBufUtils.readVarInt(buf, 5);
		if (length < 0 || length > 16384 || length > buf.readableBytes()) {
			png = new byte[0];
			return;
		}
		png = new byte[length];
		buf.readBytes(png);
	}
}
