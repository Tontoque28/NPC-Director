package com.tontoque28.npcdirector.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tontoque28.npcdirector.NpcDirector;

import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;

public final class ClientLang {

	public static final String FILE_NAME = "npcdirector-client.properties";
	public static final String PROPERTY = "lang";
	public static final String[] MODES = {"auto", "es", "en"};
	private static final String[] FILES = {"", "es_es", "en_us"};
	private static final Pattern PLACEHOLDER = Pattern.compile("%(?:(\\d+)\\$)?s|%%");

	private static int mode = -1;
	private static final Map<String, Map<String, String>> TABLES = new HashMap<>();
	private static IResourceManager loadedFor;

	private ClientLang() {
	}

	private static Path file() {
		return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
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
			String value = properties.getProperty(PROPERTY, "auto").trim().toLowerCase(Locale.ROOT);
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

	public static IFormattableTextComponent label() {
		return tr("npcdirector.gui.lang." + MODES[mode()]);
	}

	public static String shortLabel() {
		return tr("npcdirector.gui.lang." + MODES[mode()] + ".short").getString();
	}

	private static Map<String, String> table(int selected) {
		IResourceManager manager = Minecraft.getInstance().getResourceManager();
		if (manager != loadedFor) {
			TABLES.clear();
			loadedFor = manager;
		}
		Map<String, String> known = TABLES.get(FILES[selected]);
		if (known == null) {
			known = read(manager, FILES[selected]);
			TABLES.put(FILES[selected], known);
		}
		return known;
	}

	private static Map<String, String> read(IResourceManager manager, String name) {
		Map<String, String> table = new HashMap<>();
		try (IResource resource = manager.getResource(new ResourceLocation(NpcDirector.MOD_ID, "lang/" + name + ".json"));
				Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
			JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
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

	public static IFormattableTextComponent tr(String key, Object... args) {
		int selected = mode();
		if (selected == 0) {
			return new TranslationTextComponent(key, args);
		}
		String pattern = table(selected).get(key);
		if (pattern == null) {
			return new TranslationTextComponent(key, args);
		}
		return format(pattern, args);
	}

	private static IFormattableTextComponent format(String pattern, Object[] args) {
		IFormattableTextComponent result = new StringTextComponent("");
		Matcher matcher = PLACEHOLDER.matcher(pattern);
		int last = 0;
		int next = 0;
		while (matcher.find()) {
			if (matcher.start() > last) {
				result.append(new StringTextComponent(pattern.substring(last, matcher.start())));
			}
			if (matcher.group().equals("%%")) {
				result.append(new StringTextComponent("%"));
			} else {
				int index = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) - 1 : next++;
				if (index >= 0 && index < args.length) {
					Object arg = args[index];
					if (arg instanceof net.minecraft.util.text.ITextComponent) {
						result.append((net.minecraft.util.text.ITextComponent) arg);
					} else {
						result.append(new StringTextComponent(String.valueOf(arg)));
					}
				}
			}
			last = matcher.end();
		}
		if (last < pattern.length()) {
			result.append(new StringTextComponent(pattern.substring(last)));
		}
		return result;
	}
}
