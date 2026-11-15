package com.tontoque28.npcdirector.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.PacketBuffer;

public final class OpenAdminPayload {
	private final int npcEntityId;
	private final String name;
	private final int relevance;
	private final boolean targetable;
	private final String pathId;
	private final String dialogueId;
	private final List<String> paths;
	private final List<String> dialogues;
	private final String skinName;
	private final AdminFields fields;
	private final boolean dev;
	private final PathData pathData;
	private final DialogueData dialogueData;
	public OpenAdminPayload(int npcEntityId, String name, int relevance, boolean targetable, String pathId, String dialogueId, List<String> paths, List<String> dialogues, String skinName, AdminFields fields, boolean dev, PathData pathData, DialogueData dialogueData) {
		this.npcEntityId = npcEntityId;
		this.name = name;
		this.relevance = relevance;
		this.targetable = targetable;
		this.pathId = pathId;
		this.dialogueId = dialogueId;
		this.paths = paths;
		this.dialogues = dialogues;
		this.skinName = skinName;
		this.fields = fields;
		this.dev = dev;
		this.pathData = pathData;
		this.dialogueData = dialogueData;
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
	public String skinName() {
		return skinName;
	}
	public AdminFields fields() {
		return fields;
	}
	public boolean dev() {
		return dev;
	}
	public PathData pathData() {
		return pathData;
	}
	public DialogueData dialogueData() {
		return dialogueData;
	}


	public static void encode(OpenAdminPayload payload, PacketBuffer buf) {
		buf.writeVarInt(payload.npcEntityId() + 1);
		buf.writeUtf(payload.name(), 64);
		buf.writeVarInt(payload.relevance());
		buf.writeBoolean(payload.targetable());
		buf.writeUtf(payload.pathId(), 128);
		buf.writeUtf(payload.dialogueId(), 128);
		writeList(buf, payload.paths());
		writeList(buf, payload.dialogues());
		buf.writeUtf(payload.skinName(), ApplySkinPayload.NAME_MAX);
		payload.fields().write(buf);
		buf.writeBoolean(payload.dev());
		payload.pathData().write(buf);
		payload.dialogueData().write(buf);
	}

	public static OpenAdminPayload decode(PacketBuffer buf) {
		int id = buf.readVarInt() - 1;
		String name = buf.readUtf(64);
		int relevance = buf.readVarInt();
		boolean targetable = buf.readBoolean();
		String path = buf.readUtf(128);
		String dialogue = buf.readUtf(128);
		List<String> paths = readList(buf);
		List<String> dialogues = readList(buf);
		String skin = buf.readUtf(ApplySkinPayload.NAME_MAX);
		AdminFields fields = AdminFields.read(buf);
		boolean dev = buf.readBoolean();
		PathData pathData = PathData.read(buf);
		DialogueData dialogueData = DialogueData.read(buf);
		return new OpenAdminPayload(id, name, relevance, targetable, path, dialogue, paths, dialogues, skin, fields, dev, pathData, dialogueData);
	}

	private static void writeList(PacketBuffer buf, List<String> list) {
		int count = Math.min(list.size(), 256);
		buf.writeVarInt(count);
		for (int i = 0; i < count; i++) {
			buf.writeUtf(list.get(i), 128);
		}
	}

	private static List<String> readList(PacketBuffer buf) {
		int count = Math.min(buf.readVarInt(), 256);
		List<String> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			list.add(buf.readUtf(128));
		}
		return list;
	}
}
