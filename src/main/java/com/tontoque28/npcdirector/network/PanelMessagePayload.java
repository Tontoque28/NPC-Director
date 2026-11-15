package com.tontoque28.npcdirector.network;

import com.tontoque28.npcdirector.NpcDirector;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PanelMessagePayload(Component message) implements CustomPacketPayload {

	public static final Type<PanelMessagePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(NpcDirector.MOD_ID, "panel_message"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PanelMessagePayload> STREAM_CODEC = ComponentSerialization.STREAM_CODEC.map(PanelMessagePayload::new, PanelMessagePayload::message);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
