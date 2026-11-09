package com.tontoque28.npcdirector.network;

import java.util.ArrayList;
import java.util.List;

import com.tontoque28.npcdirector.NpcDirector;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenAdminPayload(int npcEntityId, String name, int relevance, boolean targetable, String pathId, String dialogueId, List<String> paths, List<String> dialogues) implements CustomPacketPayload {

	public static final Type<OpenAdminPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(NpcDirector.MOD_ID, "open_admin"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenAdminPayload> CODEC = StreamCodec.ofMember(OpenAdminPayload::encode, OpenAdminPayload::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
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
