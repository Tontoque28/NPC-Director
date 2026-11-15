package com.tontoque28.npcdirector.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public final class ClientSkins {

	public record Info(Supplier<PlayerSkin> skin, boolean slim) {
	}

	private static final Info DEFAULT = new Info(DefaultPlayerSkin::getDefaultSkin, false);
	private static final Map<String, Info> CACHE = new HashMap<>();

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
		Info built = build(npc, texture);
		CACHE.put(texture, built);
		return built;
	}

	public static PlayerSkin resolve(QuestNpcEntity npc) {
		Info info = of(npc);
		PlayerSkin base = info.skin().get();
		PlayerModelType model = slim(npc, info) ? PlayerModelType.SLIM : PlayerModelType.WIDE;
		if (base.model() == model) {
			return base;
		}
		return new PlayerSkin(base.body(), base.cape(), base.elytra(), model, base.secure());
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
		try {
			JsonObject root = JsonParser.parseString(new String(Base64.getDecoder().decode(texture), StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject skin = root.getAsJsonObject("textures").getAsJsonObject("SKIN");
			if (skin == null || !skin.has("url") || !trusted(skin.get("url").getAsString())) {
				return DEFAULT;
			}
			boolean slim = skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")
					&& "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());
			UUID id;
			try {
				id = UUID.fromString(npc.getSyncedSkinUuid());
			} catch (IllegalArgumentException exception) {
				id = UUID.nameUUIDFromBytes(("NPCDirector:" + texture).getBytes(StandardCharsets.UTF_8));
			}
			String name = npc.getSyncedSkinName();
			GameProfile profile = new GameProfile(id, name.isEmpty() ? "NPC" : name,
					new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", texture))));
			Supplier<PlayerSkin> lookup = Minecraft.getInstance().getSkinManager().createLookup(profile, false);
			return new Info(lookup, slim);
		} catch (RuntimeException exception) {
			return DEFAULT;
		}
	}
}
