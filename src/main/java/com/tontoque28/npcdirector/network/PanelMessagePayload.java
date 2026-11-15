package com.tontoque28.npcdirector.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;

public record PanelMessagePayload(Component message) {

	public static final StreamCodec<RegistryFriendlyByteBuf, PanelMessagePayload> STREAM_CODEC = ComponentSerialization.STREAM_CODEC.map(PanelMessagePayload::new, PanelMessagePayload::message);

}
