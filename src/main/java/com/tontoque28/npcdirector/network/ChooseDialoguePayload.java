package com.tontoque28.npcdirector.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class ChooseDialoguePayload implements IMessage {

	private int optionIndex;

	public ChooseDialoguePayload() {
	}

	public ChooseDialoguePayload(int optionIndex) {
		this.optionIndex = optionIndex;
	}

	public int optionIndex() {
		return optionIndex;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeVarInt(buf, optionIndex, 5);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		optionIndex = ByteBufUtils.readVarInt(buf, 5);
	}
}
