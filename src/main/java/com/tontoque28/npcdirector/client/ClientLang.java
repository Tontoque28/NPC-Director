package com.tontoque28.npcdirector.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.tontoque28.npcdirector.NpcDirector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

public final class ClientLang {

	public static final String FILE_NAME = "npcdirector-client.properties";
	private static final String[] MODES = {"auto", "es", "en"};

	private static String mode = null;
	private static final Map<String, Map<String, String>> CACHE = new HashMap<>();

	private ClientLang() {
	}

	private static File file() {
		return new File(Loader.instance().getConfigDir(), FILE_NAME);
	}

	public static synchronized String mode() {
		if (mode != null) {
			return mode;
		}
		mode = "auto";
		File file = file();
		if (file.isFile()) {
			Properties properties = new Properties();
			try (InputStream in = new FileInputStream(file)) {
				properties.load(in);
				String value = properties.getProperty("lang", "auto").trim();
				for (String candidate : MODES) {
					if (candidate.equals(value)) {
						mode = value;
					}
				}
			} catch (IOException | RuntimeException exception) {
				NpcDirector.LOGGER.warn("Could not read {}: {}", FILE_NAME, exception.getMessage());
			}
		}
		return mode;
	}

	public static synchronized void cycle() {
		String current = mode();
		int next = 0;
		for (int i = 0; i < MODES.length; i++) {
			if (MODES[i].equals(current)) {
				next = (i + 1) % MODES.length;
			}
		}
		mode = MODES[next];
		Properties properties = new Properties();
		properties.setProperty("lang", mode);
		try {
			File file = file();
			file.getParentFile().mkdirs();
			try (OutputStream out = new FileOutputStream(file)) {
				properties.store(out, "NPCDirector client: lang=auto, es or en");
			}
		} catch (IOException exception) {
			NpcDirector.LOGGER.warn("Could not write {}: {}", FILE_NAME, exception.getMessage());
		}
	}

	private static Map<String, String> table(String code) {
		Map<String, String> loaded = CACHE.get(code);
		if (loaded != null) {
			return loaded;
		}
		loaded = new HashMap<>();
		String name = code.equals("es") ? "es_es" : "en_us";
		try (InputStream in = Minecraft.getMinecraft().getResourceManager()
				.getResource(new ResourceLocation(NpcDirector.MOD_ID, "lang/" + name + ".lang")).getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				int eq = line.indexOf('=');
				if (eq > 0) {
					loaded.put(line.substring(0, eq), line.substring(eq + 1));
				}
			}
		} catch (IOException | RuntimeException exception) {
			NpcDirector.LOGGER.warn("Could not read lang {}: {}", name, exception.getMessage());
		}
		CACHE.put(code, loaded);
		return loaded;
	}

	public static String tr(String key, Object... args) {
		String current = mode();
		if (current.equals("auto")) {
			return I18n.format(key, args);
		}
		String text = table(current).get(key);
		if (text == null) {
			return I18n.format(key, args);
		}
		try {
			return String.format(text, args);
		} catch (RuntimeException exception) {
			return text;
		}
	}

	public static String label() {
		return tr("npcdirector.gui.lang", tr("npcdirector.gui.lang." + mode()));
	}
}
