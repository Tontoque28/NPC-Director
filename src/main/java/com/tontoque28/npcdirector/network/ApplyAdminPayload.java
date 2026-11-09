package com.tontoque28.npcdirector.network;

import com.tontoque28.npcdirector.NpcDirector;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ApplyAdminPayload(int npcEntityId, String name, int relevance, boolean targetable, String pathId, String dialogueId) implements CustomPacketPayload {

	public static final Type<ApplyAdminPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(NpcDirector.MOD_ID, "apply_admin"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ApplyAdminPayload> CODEC = StreamCodec.ofMember(ApplyAdminPayload::encode, ApplyAdminPayload::decode);

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
	}

	public static ApplyAdminPayload decode(FriendlyByteBuf buf) {
		return new ApplyAdminPayload(buf.readVarInt(), buf.readUtf(64), buf.readVarInt(), buf.readBoolean(), buf.readUtf(128), buf.readUtf(128));
	}
}
