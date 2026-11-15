package com.tontoque28.npcdirector.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PanelMessagePayload implements IMessage {

	private String key = "";

	public PanelMessagePayload() {
	}

	public PanelMessagePayload(String key) {
		this.key = key;
	}

	public String key() {
		return key;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, key.length() > 128 ? key.substring(0, 128) : key);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		String value = ByteBufUtils.readUTF8String(buf);
		key = value.length() > 128 ? value.substring(0, 128) : value;
	}
}
