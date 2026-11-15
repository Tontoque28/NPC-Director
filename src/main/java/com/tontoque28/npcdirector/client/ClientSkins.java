package com.tontoque28.npcdirector.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class ClientSkins {

	public static final class Info {
	private final ResourceLocation texture;
	private final boolean slim;
	public Info(ResourceLocation texture, boolean slim) {
		this.texture = texture;
		this.slim = slim;
	}
	public ResourceLocation texture() {
		return texture;
	}
	public boolean slim() {
		return slim;
	}

	}

	private static final ResourceLocation STEVE = new ResourceLocation("textures/entity/steve.png");
	private static final Info DEFAULT = new Info(STEVE, false);
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
		UUID id = null;
		try {
			id = UUID.fromString(npc.getSyncedSkinUuid());
		} catch (IllegalArgumentException exception) {
			id = null;
		}
		String name = npc.getSyncedSkinName();
		GameProfile profile = new GameProfile(id, name.isEmpty() ? "NPC" : name);
		profile.getProperties().put("textures", new Property("textures", texture));
		try {
			Minecraft minecraft = Minecraft.getInstance();
			Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraft.getSkinManager().getInsecureSkinInformation(profile);
			MinecraftProfileTexture skin = map.get(MinecraftProfileTexture.Type.SKIN);
			if (skin == null || !trusted(skin.getUrl())) {
				return DEFAULT;
			}
			ResourceLocation location = minecraft.getSkinManager().registerTexture(skin, MinecraftProfileTexture.Type.SKIN);
			return new Info(location, "slim".equals(skin.getMetadata("model")));
		} catch (RuntimeException exception) {
			return DEFAULT;
		}
	}
}
