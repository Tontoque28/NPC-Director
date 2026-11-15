package com.tontoque28.npcdirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.tontoque28.npcdirector.NpcDirector;

public record SaveDialoguePayload(int npcEntityId, String name, DialogueData dialogue) implements CustomPacketPayload {

	public static final Type<SaveDialoguePayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "save_dialogue"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SaveDialoguePayload> STREAM_CODEC = StreamCodec.ofMember(SaveDialoguePayload::encode, SaveDialoguePayload::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void encode(SaveDialoguePayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.name(), 64);
		payload.dialogue().write(buf);
	}

	public static SaveDialoguePayload decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		String name = buf.readUtf(64);
		return new SaveDialoguePayload(id, name, DialogueData.read(buf));
	}
}
