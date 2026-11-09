package com.tontoque28.npcdirector.network;

import com.tontoque28.npcdirector.NpcDirector;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChooseDialoguePayload(int optionIndex) implements CustomPacketPayload {

	public static final Type<ChooseDialoguePayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "choose_dialogue"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ChooseDialoguePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			ChooseDialoguePayload::optionIndex,
			ChooseDialoguePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
