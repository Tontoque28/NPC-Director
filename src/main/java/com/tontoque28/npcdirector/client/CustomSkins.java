package com.tontoque28.npcdirector.client;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.tontoque28.npcdirector.NpcDirector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public final class CustomSkins {

	public record Entry(ResourceLocation id, ClientSkins.Info info) {
	}

	private static final int MAX_TEXTURES = 32;
	private static final Map<String, Entry> TEXTURES = new LinkedHashMap<>();

	private CustomSkins() {
	}

	public static Entry get(String hash) {
		return TEXTURES.get(hash);
	}

	public static void clear() {
		Minecraft minecraft = Minecraft.getInstance();
		for (Entry entry : TEXTURES.values()) {
			minecraft.getTextureManager().release(entry.id());
		}
		TEXTURES.clear();
	}

	public static void receive(String hash, byte[] png) {
		if (TEXTURES.containsKey(hash) || !hash.matches("[0-9a-f]{1,32}")) {
			return;
		}
		NativeImage image;
		try {
			image = NativeImage.read(png);
		} catch (IOException | RuntimeException exception) {
			NpcDirector.LOGGER.warn("Custom skin {} unreadable: {}", hash, exception.getMessage());
			return;
		}
		if (image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
			image.close();
			return;
		}
		boolean legacy = image.getHeight() == 32;
		boolean slim = !legacy && transparent(image, 54, 20) && transparent(image, 55, 20) && transparent(image, 54, 31) && transparent(image, 55, 31);
		NativeImage ready = legacy ? convertLegacy(image) : image;
		setOpaque(ready, 0, 0, 32, 16);
		setOpaque(ready, 0, 16, 64, 32);
		setOpaque(ready, 16, 48, 48, 64);
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "custom_skin/" + hash);
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.getTextureManager().register(id, new DynamicTexture(ready));
		TEXTURES.put(hash, new Entry(id, new ClientSkins.Info(id, slim)));
		while (TEXTURES.size() > MAX_TEXTURES) {
			String oldest = TEXTURES.keySet().iterator().next();
			minecraft.getTextureManager().release(TEXTURES.remove(oldest).id());
		}
	}

	private static boolean transparent(NativeImage image, int x, int y) {
		return (image.getPixel(x, y) >>> 24) == 0;
	}

	private static void setOpaque(NativeImage image, int x0, int y0, int x1, int y1) {
		for (int x = x0; x < x1; x++) {
			for (int y = y0; y < y1; y++) {
				image.setPixel(x, y, image.getPixel(x, y) | 0xFF000000);
			}
		}
	}

	private static NativeImage convertLegacy(NativeImage source) {
		NativeImage image = new NativeImage(64, 64, true);
		image.copyFrom(source);
		source.close();
		image.fillRect(0, 32, 64, 32, 0);
		image.copyRect(4, 16, 16, 32, 4, 4, true, false);
		image.copyRect(8, 16, 16, 32, 4, 4, true, false);
		image.copyRect(0, 20, 24, 32, 4, 12, true, false);
		image.copyRect(4, 20, 16, 32, 4, 12, true, false);
		image.copyRect(8, 20, 8, 32, 4, 12, true, false);
		image.copyRect(12, 20, 16, 32, 4, 12, true, false);
		image.copyRect(44, 16, -8, 32, 4, 4, true, false);
		image.copyRect(48, 16, -8, 32, 4, 4, true, false);
		image.copyRect(40, 20, 0, 32, 4, 12, true, false);
		image.copyRect(44, 20, -8, 32, 4, 12, true, false);
		image.copyRect(48, 20, -16, 32, 4, 12, true, false);
		image.copyRect(52, 20, -8, 32, 4, 12, true, false);
		clearSolidHat(image, 32, 0, 64, 32);
		return image;
	}

	private static void clearSolidHat(NativeImage image, int x0, int y0, int x1, int y1) {
		for (int x = x0; x < x1; x++) {
			for (int y = y0; y < y1; y++) {
				if ((image.getPixel(x, y) >>> 24) < 128) {
					return;
				}
			}
		}
		for (int x = x0; x < x1; x++) {
			for (int y = y0; y < y1; y++) {
				image.setPixel(x, y, image.getPixel(x, y) & 0x00FFFFFF);
			}
		}
	}
}
