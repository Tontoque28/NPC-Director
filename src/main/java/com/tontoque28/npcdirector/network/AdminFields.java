package com.tontoque28.npcdirector.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;

public record AdminFields(float scale, int armModel, boolean nameVisible, int nameColor, List<String> equip,
		boolean lookAtPlayer, boolean stationary, boolean invulnerable, float patrolSpeed, boolean sitting,
		String greeting, int greetRange, String soundId, float soundVolume, float soundPitch, boolean enabled) {

	public static final int EQUIP_COUNT = 6;
	public static final int EQUIP_MAX = 64;
	public static final int GREETING_MAX = 256;
	public static final int SOUND_MAX = 128;

	public static AdminFields defaults() {
		List<String> equip = new ArrayList<>();
		for (int i = 0; i < EQUIP_COUNT; i++) {
			equip.add("");
		}
		return new AdminFields(1.0F, 0, true, 15, equip, true, false, false, 1.0F, false, "", 5, "", 1.0F, 1.0F, true);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeFloat(scale);
		buf.writeVarInt(armModel);
		buf.writeBoolean(nameVisible);
		buf.writeVarInt(nameColor);
		for (int i = 0; i < EQUIP_COUNT; i++) {
			buf.writeUtf(i < equip.size() ? equip.get(i) : "", EQUIP_MAX);
		}
		buf.writeBoolean(lookAtPlayer);
		buf.writeBoolean(stationary);
		buf.writeBoolean(invulnerable);
		buf.writeFloat(patrolSpeed);
		buf.writeBoolean(sitting);
		buf.writeUtf(greeting, GREETING_MAX);
		buf.writeVarInt(greetRange);
		buf.writeUtf(soundId, SOUND_MAX);
		buf.writeFloat(soundVolume);
		buf.writeFloat(soundPitch);
		buf.writeBoolean(enabled);
	}

	public static AdminFields read(FriendlyByteBuf buf) {
		float scale = buf.readFloat();
		int armModel = buf.readVarInt();
		boolean nameVisible = buf.readBoolean();
		int nameColor = buf.readVarInt();
		List<String> equip = new ArrayList<>();
		for (int i = 0; i < EQUIP_COUNT; i++) {
			equip.add(buf.readUtf(EQUIP_MAX));
		}
		boolean lookAtPlayer = buf.readBoolean();
		boolean stationary = buf.readBoolean();
		boolean invulnerable = buf.readBoolean();
		float patrolSpeed = buf.readFloat();
		boolean sitting = buf.readBoolean();
		String greeting = buf.readUtf(GREETING_MAX);
		int greetRange = buf.readVarInt();
		String soundId = buf.readUtf(SOUND_MAX);
		float soundVolume = buf.readFloat();
		float soundPitch = buf.readFloat();
		boolean enabled = buf.readBoolean();
		return new AdminFields(scale, armModel, nameVisible, nameColor, equip, lookAtPlayer, stationary, invulnerable, patrolSpeed, sitting, greeting, greetRange, soundId, soundVolume, soundPitch, enabled);
	}
}
