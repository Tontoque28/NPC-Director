package com.tontoque28.npcdirector.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;

public record PathData(boolean loop, List<int[]> points) {

	public static final int MAX_POINTS = 32;

	public static PathData empty() {
		return new PathData(false, new ArrayList<>());
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(loop);
		int count = Math.min(points.size(), MAX_POINTS);
		buf.writeVarInt(count);
		for (int i = 0; i < count; i++) {
			int[] point = points.get(i);
			buf.writeInt(point[0]);
			buf.writeInt(point[1]);
			buf.writeInt(point[2]);
		}
	}

	public static PathData read(FriendlyByteBuf buf) {
		boolean loop = buf.readBoolean();
		int count = buf.readVarInt();
		if (count < 0 || count > 256) {
			throw new IllegalArgumentException("demasiados puntos");
		}
		List<int[]> points = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			points.add(new int[] {buf.readInt(), buf.readInt(), buf.readInt()});
		}
		return new PathData(loop, points);
	}
}
