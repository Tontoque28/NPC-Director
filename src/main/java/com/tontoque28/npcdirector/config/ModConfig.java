package com.tontoque28.npcdirector.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import com.tontoque28.npcdirector.NpcDirector;

import net.minecraftforge.fml.common.Loader;

public class ModConfig {

	public static final String FILE_NAME = "npcdirector.properties";
	private static final String KEY = "devMode";

	private static boolean devMode = true;
	private static boolean loaded = false;

	private static File file() {
		return new File(Loader.instance().getConfigDir(), FILE_NAME);
	}

	private static synchronized void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		File file = file();
		if (!file.isFile()) {
			return;
		}
		Properties properties = new Properties();
		try (InputStream in = new FileInputStream(file)) {
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
		File file = file();
		Properties properties = new Properties();
		properties.setProperty(KEY, Boolean.toString(value));
		try {
			file.getParentFile().mkdirs();
			try (OutputStream out = new FileOutputStream(file)) {
				properties.store(out, "NPCDirector: devMode=true (developer) or false (player)");
			}
		} catch (IOException exception) {
			NpcDirector.LOGGER.warn("Could not write {}: {}", FILE_NAME, exception.getMessage());
		}
	}
}
