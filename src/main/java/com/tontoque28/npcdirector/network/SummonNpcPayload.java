package com.tontoque28.npcdirector.network;

import com.tontoque28.npcdirector.NpcDirector;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SummonNpcPayload() implements CustomPacketPayload {

	public static final Type<SummonNpcPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "summon_npc"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SummonNpcPayload> CODEC = StreamCodec.unit(new SummonNpcPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
