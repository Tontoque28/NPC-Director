package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import com.tontoque28.npcdirector.NpcDirector;

public record DeleteNpcPayload(int npcEntityId) implements CustomPacketPayload {

	public static final Type<DeleteNpcPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(NpcDirector.MOD_ID, "delete_npc"));

	public static final StreamCodec<RegistryFriendlyByteBuf, DeleteNpcPayload> STREAM_CODEC = StreamCodec.ofMember(DeleteNpcPayload::encode, DeleteNpcPayload::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void encode(DeleteNpcPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
	}

	public static DeleteNpcPayload decode(FriendlyByteBuf buf) {
		return new DeleteNpcPayload(buf.readVarInt());
	}
}
