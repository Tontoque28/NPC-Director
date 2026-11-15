package com.tontoque28.npcdirector.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.tontoque28.npcdirector.NpcDirector;

public record OpenAdminPayload(int npcEntityId, String name, int relevance, boolean targetable, String pathId, String dialogueId, List<String> paths, List<String> dialogues, String skinName, AdminFields fields, boolean dev, PathData pathData, DialogueData dialogueData) implements CustomPacketPayload {

	public static final Type<OpenAdminPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "open_admin"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenAdminPayload> STREAM_CODEC = StreamCodec.ofMember(OpenAdminPayload::encode, OpenAdminPayload::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void encode(OpenAdminPayload payload, FriendlyByteBuf buf) {
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

	public static OpenAdminPayload decode(FriendlyByteBuf buf) {
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

	private static void writeList(FriendlyByteBuf buf, List<String> list) {
		int count = Math.min(list.size(), 256);
		buf.writeVarInt(count);
		for (int i = 0; i < count; i++) {
			buf.writeUtf(list.get(i), 128);
		}
	}

	private static List<String> readList(FriendlyByteBuf buf) {
		int count = Math.min(buf.readVarInt(), 256);
		List<String> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			list.add(buf.readUtf(128));
		}
		return list;
	}
}
