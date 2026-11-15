package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.tontoque28.npcdirector.NpcDirector;

public record ApplySkinPayload(int npcEntityId, String skinName) implements CustomPacketPayload {

	public static final Type<ApplySkinPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "apply_skin"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ApplySkinPayload> STREAM_CODEC = StreamCodec.ofMember(ApplySkinPayload::encode, ApplySkinPayload::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static final int NAME_MAX = 32;

	public static void encode(ApplySkinPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.skinName(), NAME_MAX);
	}

	public static ApplySkinPayload decode(FriendlyByteBuf buf) {
		return new ApplySkinPayload(buf.readVarInt(), buf.readUtf(NAME_MAX));
	}
}
