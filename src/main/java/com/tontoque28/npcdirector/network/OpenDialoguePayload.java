package com.tontoque28.npcdirector.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record OpenDialoguePayload(String text, List<String> optionTexts) {

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenDialoguePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			OpenDialoguePayload::text,
			ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
			OpenDialoguePayload::optionTexts,
			OpenDialoguePayload::new
	);
}
