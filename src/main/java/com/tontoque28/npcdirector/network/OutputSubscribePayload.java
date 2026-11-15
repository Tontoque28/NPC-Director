package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.tontoque28.npcdirector.NpcDirector;

public record OutputSubscribePayload(boolean on) implements CustomPacketPayload {

	public static final Type<OutputSubscribePayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "output_subscribe"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OutputSubscribePayload> STREAM_CODEC = StreamCodec.ofMember(OutputSubscribePayload::encode, OutputSubscribePayload::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void encode(OutputSubscribePayload payload, FriendlyByteBuf buf) {
		buf.writeBoolean(payload.on());
	}

	public static OutputSubscribePayload decode(FriendlyByteBuf buf) {
		return new OutputSubscribePayload(buf.readBoolean());
	}
}
