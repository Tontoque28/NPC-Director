package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.tontoque28.npcdirector.NpcDirector;

public record SummonNpcPayload() implements CustomPacketPayload {

	public static final Type<SummonNpcPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "summon_npc"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SummonNpcPayload> STREAM_CODEC = StreamCodec.ofMember(SummonNpcPayload::encode, SummonNpcPayload::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void encode(SummonNpcPayload payload, FriendlyByteBuf buf) {
	}

	public static SummonNpcPayload decode(FriendlyByteBuf buf) {
		return new SummonNpcPayload();
	}
}
