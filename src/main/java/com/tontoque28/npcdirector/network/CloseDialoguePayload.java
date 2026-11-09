package com.tontoque28.npcdirector.network;

import com.tontoque28.npcdirector.NpcDirector;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CloseDialoguePayload() implements CustomPacketPayload {

	public static final Type<CloseDialoguePayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "close_dialogue"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CloseDialoguePayload> CODEC = StreamCodec.unit(new CloseDialoguePayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
