package com.tontoque28.npcdirector.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import com.tontoque28.npcdirector.NpcDirector;

public record CloseDialoguePayload() implements CustomPacketPayload {

	public static final Type<CloseDialoguePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(NpcDirector.MOD_ID, "close_dialogue"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CloseDialoguePayload> STREAM_CODEC = StreamCodec.unit(new CloseDialoguePayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
