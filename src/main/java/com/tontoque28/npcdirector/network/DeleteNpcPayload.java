package com.tontoque28.npcdirector.network;

import com.tontoque28.npcdirector.NpcDirector;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DeleteNpcPayload(int npcEntityId) implements CustomPacketPayload {

	public static final Type<DeleteNpcPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(NpcDirector.MOD_ID, "delete_npc"));

	public static final StreamCodec<RegistryFriendlyByteBuf, DeleteNpcPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			DeleteNpcPayload::npcEntityId,
			DeleteNpcPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
