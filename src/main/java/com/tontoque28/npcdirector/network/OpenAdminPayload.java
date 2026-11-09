package com.tontoque28.npcdirector.network;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class OpenAdminPayload implements IMessage {

	private int npcEntityId = -1;
	private String name = "";
	private int relevance;
	private boolean targetable;
	private String pathId = "";
	private String dialogueId = "";
	private List<String> paths = new ArrayList<>();
	private List<String> dialogues = new ArrayList<>();

	public OpenAdminPayload() {
	}

	public OpenAdminPayload(int npcEntityId, String name, int relevance, boolean targetable, String pathId, String dialogueId, List<String> paths, List<String> dialogues) {
		this.npcEntityId = npcEntityId;
		this.name = name;
		this.relevance = relevance;
		this.targetable = targetable;
		this.pathId = pathId;
		this.dialogueId = dialogueId;
		this.paths = paths;
		this.dialogues = dialogues;
	}

	public int npcEntityId() {
		return npcEntityId;
	}

	public String name() {
		return name;
	}

	public int relevance() {
		return relevance;
	}

	public boolean targetable() {
		return targetable;
	}

	public String pathId() {
		return pathId;
	}

	public String dialogueId() {
		return dialogueId;
	}

	public List<String> paths() {
		return paths;
	}

	public List<String> dialogues() {
		return dialogues;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeVarInt(buf, npcEntityId + 1, 5);
		ByteBufUtils.writeUTF8String(buf, name);
		ByteBufUtils.writeVarInt(buf, relevance, 5);
		buf.writeBoolean(targetable);
		ByteBufUtils.writeUTF8String(buf, pathId);
		ByteBufUtils.writeUTF8String(buf, dialogueId);
		writeList(buf, paths);
		writeList(buf, dialogues);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		npcEntityId = ByteBufUtils.readVarInt(buf, 5) - 1;
		name = ByteBufUtils.readUTF8String(buf);
		relevance = ByteBufUtils.readVarInt(buf, 5);
		targetable = buf.readBoolean();
		pathId = ByteBufUtils.readUTF8String(buf);
		dialogueId = ByteBufUtils.readUTF8String(buf);
		paths = readList(buf);
		dialogues = readList(buf);
	}

	private static void writeList(ByteBuf buf, List<String> list) {
		ByteBufUtils.writeVarInt(buf, list.size(), 5);
		for (String value : list) {
			ByteBufUtils.writeUTF8String(buf, value);
		}
	}

	private static List<String> readList(ByteBuf buf) {
		int count = ByteBufUtils.readVarInt(buf, 5);
		List<String> list = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			list.add(ByteBufUtils.readUTF8String(buf));
		}
		return list;
	}
}
