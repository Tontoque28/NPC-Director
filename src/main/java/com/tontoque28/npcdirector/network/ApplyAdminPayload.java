package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import com.tontoque28.npcdirector.NpcDirector;

public record ApplyAdminPayload(int npcEntityId, String name, int relevance, boolean targetable, String pathId, String dialogueId, AdminFields fields) implements CustomPacketPayload {

	public static final Type<ApplyAdminPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(NpcDirector.MOD_ID, "apply_admin"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ApplyAdminPayload> STREAM_CODEC = StreamCodec.ofMember(ApplyAdminPayload::encode, ApplyAdminPayload::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void encode(ApplyAdminPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.name(), 64);
		buf.writeVarInt(payload.relevance());
		buf.writeBoolean(payload.targetable());
		buf.writeUtf(payload.pathId(), 128);
		buf.writeUtf(payload.dialogueId(), 128);
		payload.fields().write(buf);
	}

	public static ApplyAdminPayload decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		String name = buf.readUtf(64);
		int relevance = buf.readVarInt();
		boolean targetable = buf.readBoolean();
		String path = buf.readUtf(128);
		String dialogue = buf.readUtf(128);
		return new ApplyAdminPayload(id, name, relevance, targetable, path, dialogue, AdminFields.read(buf));
	}
}
