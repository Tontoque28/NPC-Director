package com.tontoque28.npcdirector.network;

import com.tontoque28.npcdirector.NpcDirector;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AssignPathPayload(int npcEntityId, String pathId, boolean radius) implements CustomPacketPayload {

	public static final Type<AssignPathPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "assign_path"));

	public static final StreamCodec<RegistryFriendlyByteBuf, AssignPathPayload> STREAM_CODEC = StreamCodec.ofMember(AssignPathPayload::encode, AssignPathPayload::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

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
