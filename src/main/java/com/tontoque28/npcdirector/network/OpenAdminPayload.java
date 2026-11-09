package com.tontoque28.npcdirector.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public final class OpenAdminPayload {

	private final int npcEntityId;
	private final String name;
	private final int relevance;
	private final boolean targetable;
	private final String pathId;
	private final String dialogueId;
	private final List<String> paths;
	private final List<String> dialogues;

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

	public static void encode(OpenAdminPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId() + 1);
		buf.writeUtf(payload.name());
		buf.writeVarInt(payload.relevance());
		buf.writeBoolean(payload.targetable());
		buf.writeUtf(payload.pathId());
		buf.writeUtf(payload.dialogueId());
		writeList(buf, payload.paths());
		writeList(buf, payload.dialogues());
	}

	public static OpenAdminPayload decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt() - 1;
		String name = buf.readUtf();
		int relevance = buf.readVarInt();
		boolean targetable = buf.readBoolean();
		String path = buf.readUtf();
		String dialogue = buf.readUtf();
		List<String> paths = readList(buf);
		List<String> dialogues = readList(buf);
		return new OpenAdminPayload(id, name, relevance, targetable, path, dialogue, paths, dialogues);
	}

	private static void writeList(FriendlyByteBuf buf, List<String> list) {
		buf.writeVarInt(list.size());
		for (String value : list) {
			buf.writeUtf(value);
		}
	}

	private static List<String> readList(FriendlyByteBuf buf) {
		int count = buf.readVarInt();
		List<String> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			list.add(buf.readUtf());
		}
		return list;
	}
}
