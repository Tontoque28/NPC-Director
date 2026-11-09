package com.tontoque28.npcdirector.network;

import com.tontoque28.npcdirector.NpcDirector;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record OpenDialoguePayload(String text, List<String> optionTexts) implements CustomPacketPayload {

	public static final Type<OpenDialoguePayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "open_dialogue"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenDialoguePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			OpenDialoguePayload::text,
			ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
			OpenDialoguePayload::optionTexts,
			OpenDialoguePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
