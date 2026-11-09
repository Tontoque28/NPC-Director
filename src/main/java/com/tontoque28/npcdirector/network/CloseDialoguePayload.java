package com.tontoque28.npcdirector.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record CloseDialoguePayload() {

	public static final StreamCodec<RegistryFriendlyByteBuf, CloseDialoguePayload> STREAM_CODEC = StreamCodec.unit(new CloseDialoguePayload());
}
