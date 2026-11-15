package com.tontoque28.npcdirector.network;


import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record AssignPathPayload(int npcEntityId, String pathId, boolean radius) {

	public static final StreamCodec<RegistryFriendlyByteBuf, AssignPathPayload> STREAM_CODEC = StreamCodec.ofMember(AssignPathPayload::encode, AssignPathPayload::decode);

	public static void encode(AssignPathPayload payload, FriendlyByteBuf buf) {
		buf.writeVarInt(payload.npcEntityId());
		buf.writeUtf(payload.pathId(), 128);
		buf.writeBoolean(payload.radius());
	}

	public static AssignPathPayload decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		String path = buf.readUtf(128);
		return new AssignPathPayload(id, path, buf.readBoolean());
	}
}
