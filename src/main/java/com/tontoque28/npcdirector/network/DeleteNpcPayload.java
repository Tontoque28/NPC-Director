package com.tontoque28.npcdirector.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class DeleteNpcPayload implements IMessage {

	private int npcEntityId;

	public DeleteNpcPayload() {
	}

	public DeleteNpcPayload(int npcEntityId) {
		this.npcEntityId = npcEntityId;
	}

	public int npcEntityId() {
		return npcEntityId;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeVarInt(buf, npcEntityId, 5);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		npcEntityId = ByteBufUtils.readVarInt(buf, 5);
	}
}
