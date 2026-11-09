package com.tontoque28.npcdirector.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ChooseDialoguePayload(int optionIndex) {

	public static final StreamCodec<RegistryFriendlyByteBuf, ChooseDialoguePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			ChooseDialoguePayload::optionIndex,
			ChooseDialoguePayload::new
	);
}
