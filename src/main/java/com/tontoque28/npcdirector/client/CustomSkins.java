package com.tontoque28.npcdirector.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.tontoque28.npcdirector.NpcDirector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

public final class CustomSkins {

	private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

	private CustomSkins() {
	}

	public static ResourceLocation get(String hash) {
		if (hash == null || hash.isEmpty()) {
			return null;
		}
		return TEXTURES.get(hash);
	}

	public static void register(String hash, byte[] png) {
		if (hash == null || hash.isEmpty() || png == null || png.length == 0 || TEXTURES.containsKey(hash)) {
			return;
		}
		try {
			javax.imageio.ImageIO.setUseCache(false);
			BufferedImage image = javax.imageio.ImageIO.read(new ByteArrayInputStream(png));
			if (image == null || image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
				return;
			}
			BufferedImage parsed = new ImageBufferDownload().parseUserSkin(image);
			ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
					.getDynamicTextureLocation("npcdirector_skin_" + hash, new DynamicTexture(parsed));
			TEXTURES.put(hash, location);
		} catch (IOException | RuntimeException exception) {
			NpcDirector.LOGGER.warn("Could not register imported skin {}: {}", hash, exception.getMessage());
		}
	}
}
