package com.tontoque28.npcdirector.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class ImportSkinPayload implements IMessage {

	public static final int CHUNK_MAX = 4096;

	private int npcEntityId;
	private int sendId;
	private int index;
	private int total;
	private int totalBytes;
	private byte[] chunk = new byte[0];

	public ImportSkinPayload() {
	}

	public ImportSkinPayload(int npcEntityId, int sendId, int index, int total, int totalBytes, byte[] chunk) {
		this.npcEntityId = npcEntityId;
		this.sendId = sendId;
		this.index = index;
		this.total = total;
		this.totalBytes = totalBytes;
		this.chunk = chunk;
	}

	public int npcEntityId() {
		return npcEntityId;
	}

	public int sendId() {
		return sendId;
	}

	public int index() {
		return index;
	}

	public int total() {
		return total;
	}

	public int totalBytes() {
		return totalBytes;
	}

	public byte[] chunk() {
		return chunk;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeVarInt(buf, npcEntityId, 5);
		ByteBufUtils.writeVarInt(buf, sendId, 5);
		ByteBufUtils.writeVarInt(buf, index, 5);
		ByteBufUtils.writeVarInt(buf, total, 5);
		ByteBufUtils.writeVarInt(buf, totalBytes, 5);
		ByteBufUtils.writeVarInt(buf, chunk.length, 5);
		buf.writeBytes(chunk);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		npcEntityId = ByteBufUtils.readVarInt(buf, 5);
		sendId = ByteBufUtils.readVarInt(buf, 5);
		index = ByteBufUtils.readVarInt(buf, 5);
		total = ByteBufUtils.readVarInt(buf, 5);
		totalBytes = ByteBufUtils.readVarInt(buf, 5);
		int length = ByteBufUtils.readVarInt(buf, 5);
		if (length < 0 || length > CHUNK_MAX || length > buf.readableBytes()) {
			chunk = new byte[0];
			index = -1;
			return;
		}
		chunk = new byte[length];
		buf.readBytes(chunk);
	}
}
