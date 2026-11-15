package com.tontoque28.npcdirector.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tontoque28.npcdirector.NpcDirector;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ClientLang {

	public static final String FILE_NAME = "npcdirector-client.properties";
	public static final String PROPERTY = "lang";
	public static final String[] MODES = {"auto", "es", "en"};
	private static final String[] FILES = {"", "es_es", "en_us"};
	private static final Pattern PLACEHOLDER = Pattern.compile("%(?:(\\d+)\\$)?s|%%");

	private static int mode = -1;
	private static final Map<String, Map<String, String>> TABLES = new HashMap<>();
	private static ResourceManager loadedFor;

	private ClientLang() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	private static synchronized void load() {
		if (mode >= 0) {
			return;
		}
		mode = 0;
		Path file = file();
		if (!Files.isRegularFile(file)) {
			return;
		}
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(file)) {
			properties.load(in);
			String value = properties.getProperty(PROPERTY, "auto").trim().toLowerCase();
			for (int i = 0; i < MODES.length; i++) {
				if (MODES[i].equals(value)) {
					mode = i;
				}
			}
		} catch (IOException | RuntimeException exception) {
			NpcDirector.LOGGER.warn("Could not read {}: {}", FILE_NAME, exception.getMessage());
		}
	}

	private static synchronized void save() {
		Properties properties = new Properties();
		properties.setProperty(PROPERTY, MODES[mode]);
		Path file = file();
		try {
			Files.createDirectories(file.getParent());
			try (OutputStream out = Files.newOutputStream(file)) {
				properties.store(out, "NPCDirector client: lang=auto, es or en (language of the mod panel)");
			}
		} catch (IOException exception) {
			NpcDirector.LOGGER.warn("Could not write {}: {}", FILE_NAME, exception.getMessage());
		}
	}

	public static int mode() {
		load();
		return mode;
	}

	public static void set(int value) {
		load();
		mode = Math.max(0, Math.min(MODES.length - 1, value));
		save();
	}

	public static void cycle() {
		set((mode() + 1) % MODES.length);
	}

	public static MutableComponent label() {
		return tr("npcdirector.gui.lang." + MODES[mode()]);
	}

	public static String shortLabel() {
		return tr("npcdirector.gui.lang." + MODES[mode()] + ".short").getString();
	}

	private static Map<String, String> table(int selected) {
		ResourceManager manager = Minecraft.getInstance().getResourceManager();
		if (manager != loadedFor) {
			TABLES.clear();
			loadedFor = manager;
		}
		return TABLES.computeIfAbsent(FILES[selected], name -> read(manager, name));
	}

	private static Map<String, String> read(ResourceManager manager, String name) {
		Map<String, String> table = new HashMap<>();
		Optional<Resource> resource = manager.getResource(new ResourceLocation(NpcDirector.MOD_ID, "lang/" + name + ".json"));
		if (resource.isEmpty()) {
			return table;
		}
		try (Reader reader = resource.get().openAsReader()) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
				if (entry.getValue().isJsonPrimitive()) {
					table.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
		} catch (IOException | RuntimeException exception) {
			NpcDirector.LOGGER.warn("Could not read lang file {}: {}", name, exception.getMessage());
		}
		return table;
	}

	public static MutableComponent tr(String key, Object... args) {
		int selected = mode();
		if (selected == 0) {
			return Component.translatable(key, args);
		}
		String pattern = table(selected).get(key);
		if (pattern == null) {
			return Component.translatable(key, args);
		}
		return format(pattern, args);
	}

	private static MutableComponent format(String pattern, Object[] args) {
		MutableComponent result = Component.empty();
		Matcher matcher = PLACEHOLDER.matcher(pattern);
		int last = 0;
		int next = 0;
		while (matcher.find()) {
			if (matcher.start() > last) {
				result.append(Component.literal(pattern.substring(last, matcher.start())));
			}
			if (matcher.group().equals("%%")) {
				result.append(Component.literal("%"));
			} else {
				int index = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) - 1 : next++;
				if (index >= 0 && index < args.length) {
					Object arg = args[index];
					result.append(arg instanceof Component component ? component : Component.literal(String.valueOf(arg)));
				}
			}
			last = matcher.end();
		}
		if (last < pattern.length()) {
			result.append(Component.literal(pattern.substring(last)));
		}
		return result;
	}
}
