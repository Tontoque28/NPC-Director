package com.tontoque28.npcdirector.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.tontoque28.npcdirector.NpcDirector;

import net.minecraftforge.fml.loading.FMLPaths;

public class ModConfig {

	public static final String FILE_NAME = "npcdirector.properties";
	private static final String KEY = "devMode";

	private static boolean devMode = true;
	private static boolean loaded = false;

	private static Path file() {
		return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
	}

	private static synchronized void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		Path file = file();
		if (!Files.isRegularFile(file)) {
			return;
		}
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(file)) {
			properties.load(in);
			String value = properties.getProperty(KEY);
			if (value != null) {
				devMode = Boolean.parseBoolean(value.trim());
			}
		} catch (IOException | RuntimeException exception) {
			NpcDirector.LOGGER.warn("Could not read {}: {}", FILE_NAME, exception.getMessage());
		}
	}

	public static boolean isDevMode() {
		load();
		return devMode;
	}

	public static synchronized void setDevMode(boolean value) {
		load();
		devMode = value;
		Path file = file();
		Properties properties = new Properties();
		properties.setProperty(KEY, Boolean.toString(value));
		try {
			Files.createDirectories(file.getParent());
			try (OutputStream out = Files.newOutputStream(file)) {
				properties.store(out, "NPCDirector: devMode=true (developer) or false (player)");
			}
		} catch (IOException exception) {
			NpcDirector.LOGGER.warn("Could not write {}: {}", FILE_NAME, exception.getMessage());
		}
	}
}
