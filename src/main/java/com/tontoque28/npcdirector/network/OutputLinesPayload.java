package com.tontoque28.npcdirector.network;

import java.util.ArrayList;
import java.util.List;

import com.tontoque28.npcdirector.output.OutputService;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record OutputLinesPayload(List<String> lines) {

	public static final StreamCodec<RegistryFriendlyByteBuf, OutputLinesPayload> STREAM_CODEC = StreamCodec.ofMember(OutputLinesPayload::encode, OutputLinesPayload::decode);

	public static void encode(OutputLinesPayload payload, FriendlyByteBuf buf) {
		int count = Math.min(payload.lines().size(), OutputService.MAX_BATCH);
		buf.writeVarInt(count);
		for (int i = 0; i < count; i++) {
			buf.writeUtf(payload.lines().get(i), OutputService.MAX_LINE);
		}
	}

	public static OutputLinesPayload decode(FriendlyByteBuf buf) {
		int count = Math.min(buf.readVarInt(), OutputService.MAX_BATCH);
		List<String> lines = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			lines.add(buf.readUtf(OutputService.MAX_LINE));
		}
		return new OutputLinesPayload(lines);
	}
}
