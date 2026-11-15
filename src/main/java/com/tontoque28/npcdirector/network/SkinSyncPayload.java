package com.tontoque28.npcdirector.network;

import com.tontoque28.npcdirector.NpcDirector;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SkinSyncPayload(String hash, byte[] png) implements CustomPacketPayload {

	public static final Type<SkinSyncPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "skin_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SkinSyncPayload> STREAM_CODEC = StreamCodec.ofMember(SkinSyncPayload::encode, SkinSyncPayload::decode);

	public static final int MAX_PNG = 16384;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void encode(SkinSyncPayload payload, FriendlyByteBuf buf) {
		buf.writeUtf(payload.hash(), 32);
		buf.writeByteArray(payload.png());
	}

	public static SkinSyncPayload decode(FriendlyByteBuf buf) {
		String hash = buf.readUtf(32);
		return new SkinSyncPayload(hash, buf.readByteArray(MAX_PNG));
	}
}
