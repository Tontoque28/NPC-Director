package com.tontoque28.npcdirector.network;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class OpenDialoguePayload implements IMessage {

	private String text = "";
	private List<String> optionTexts = new ArrayList<>();

	public OpenDialoguePayload() {
	}

	public OpenDialoguePayload(String text, List<String> optionTexts) {
		this.text = text;
		this.optionTexts = optionTexts;
	}

	public String text() {
		return text;
	}

	public List<String> optionTexts() {
		return optionTexts;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, text);
		ByteBufUtils.writeVarInt(buf, optionTexts.size(), 5);
		for (String option : optionTexts) {
			ByteBufUtils.writeUTF8String(buf, option);
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		text = ByteBufUtils.readUTF8String(buf);
		int count = ByteBufUtils.readVarInt(buf, 5);
		optionTexts = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			optionTexts.add(ByteBufUtils.readUTF8String(buf));
		}
	}
}
