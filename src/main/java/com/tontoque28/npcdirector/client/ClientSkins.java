package com.tontoque28.npcdirector.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

public final class ClientSkins {

	public record Info(ResourceLocation texture, boolean slim) {
	}

	private static final ResourceLocation STEVE = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
	private static final Info DEFAULT = new Info(STEVE, false);
	private static final Map<String, Info> CACHE = new HashMap<>();
	private static final Map<String, GameProfile> PROFILES = new HashMap<>();

	private ClientSkins() {
	}

	public static Info of(QuestNpcEntity npc) {
		String hash = npc.getSyncedSkinPngHash();
		if (!hash.isEmpty()) {
			CustomSkins.Entry custom = CustomSkins.get(hash);
			return custom == null ? DEFAULT : custom.info();
		}
		String texture = npc.getSyncedSkinTexture();
		if (texture.isEmpty()) {
			return DEFAULT;
		}
		Info known = CACHE.get(texture);
		if (known != null) {
			return known;
		}
		if (CACHE.size() > 64) {
			CACHE.clear();
		}
		if (PROFILES.size() > 64) {
			PROFILES.clear();
		}
		Info built = build(npc, texture);
		if (built != DEFAULT) {
			CACHE.put(texture, built);
		}
		return built;
	}

	public static boolean slim(QuestNpcEntity npc, Info info) {
		int mode = npc.getSyncedArmModel();
		if (mode == 1) {
			return false;
		}
		if (mode == 2) {
			return true;
		}
		return info.slim();
	}

	public static void clear() {
		CACHE.clear();
		PROFILES.clear();
	}

	private static boolean trusted(String url) {
		try {
			java.net.URI uri = new java.net.URI(url);
			String host = uri.getHost();
			return "https".equalsIgnoreCase(uri.getScheme()) && host != null
					&& (host.equals("textures.minecraft.net") || host.endsWith(".minecraft.net") || host.endsWith(".mojang.com"));
		} catch (java.net.URISyntaxException | NullPointerException exception) {
			return false;
		}
	}

	private static Info build(QuestNpcEntity npc, String texture) {
		GameProfile profile = PROFILES.get(texture);
		if (profile == null) {
			UUID id;
			try {
				id = UUID.fromString(npc.getSyncedSkinUuid());
			} catch (IllegalArgumentException exception) {
				id = UUID.nameUUIDFromBytes(("NPCDirector:" + texture).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			}
			String name = npc.getSyncedSkinName();
			profile = new GameProfile(id, name.isEmpty() ? "NPC" : name);
			profile.getProperties().put("textures", new Property("textures", texture));
			PROFILES.put(texture, profile);
		}
		try {
			PlayerSkin skin = Minecraft.getInstance().getSkinManager().getInsecureSkin(profile);
			if (skin.textureUrl() == null) {
				return DEFAULT;
			}
			if (!trusted(skin.textureUrl())) {
				return new Info(STEVE, false);
			}
			return new Info(skin.texture(), skin.model() == PlayerSkin.Model.SLIM);
		} catch (RuntimeException exception) {
			return DEFAULT;
		}
	}
}
