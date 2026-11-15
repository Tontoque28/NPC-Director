package com.tontoque28.npcdirector.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.tontoque28.npcdirector.NpcDirector;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DataLoader {

	private static final Gson GSON = new Gson();
	private static File modSource;
	private static File worldDirectory;

	private DataLoader() {
	}

	public static void setModSource(File source) {
		modSource = source;
	}

	public static void setWorldDirectory(File directory) {
		worldDirectory = directory;
	}

	public static Map<ResourceLocation, JsonElement> load(String folder) {
		Map<ResourceLocation, JsonElement> result = new LinkedHashMap<>();
		if (modSource != null) {
			scan(modSource, folder, result);
		}
		if (worldDirectory != null) {
			File[] packs = new File(worldDirectory, "datapacks").listFiles();
			if (packs != null) {
				Arrays.sort(packs);
				for (File pack : packs) {
					scan(pack, folder, result);
				}
			}
		}
		return result;
	}

	private static void scan(File root, String folder, Map<ResourceLocation, JsonElement> result) {
		try {
			if (root.isDirectory()) {
				scanDirectory(new File(root, "data"), folder, result);
			} else if (root.isFile() && (root.getName().endsWith(".zip") || root.getName().endsWith(".jar"))) {
				scanArchive(root, folder, result);
			}
		} catch (IOException exception) {
			NpcDirector.LOGGER.warn("No se pudo leer {}: {}", root, exception.getMessage());
		}
	}

	private static void scanDirectory(File dataDir, String folder, Map<ResourceLocation, JsonElement> result) throws IOException {
		File[] namespaces = dataDir.listFiles();
		if (namespaces == null) {
			return;
		}
		Arrays.sort(namespaces);
		for (File namespace : namespaces) {
			File base = new File(namespace, folder);
			if (namespace.isDirectory() && base.isDirectory()) {
				walk(namespace.getName(), base, "", result);
			}
		}
	}

	private static void walk(String namespace, File dir, String prefix, Map<ResourceLocation, JsonElement> result) throws IOException {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		Arrays.sort(files);
		for (File file : files) {
			if (file.isDirectory()) {
				walk(namespace, file, prefix + file.getName() + "/", result);
			} else if (file.getName().endsWith(".json")) {
				String name = prefix + file.getName().substring(0, file.getName().length() - 5);
				try (InputStream stream = new java.io.FileInputStream(file)) {
					put(namespace, name, stream, result);
				}
			}
		}
	}

	private static void scanArchive(File archive, String folder, Map<ResourceLocation, JsonElement> result) throws IOException {
		try (ZipFile zip = new ZipFile(archive)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String path = entry.getName();
				if (entry.isDirectory() || !path.startsWith("data/") || !path.endsWith(".json")) {
					continue;
				}
				String rest = path.substring(5);
				int slash = rest.indexOf('/');
				if (slash <= 0) {
					continue;
				}
				String namespace = rest.substring(0, slash);
				String inner = rest.substring(slash + 1);
				if (!inner.startsWith(folder + "/")) {
					continue;
				}
				String name = inner.substring(folder.length() + 1, inner.length() - 5);
				try (InputStream stream = zip.getInputStream(entry)) {
					put(namespace, name, stream, result);
				}
			}
		}
	}

	private static void put(String namespace, String name, InputStream stream, Map<ResourceLocation, JsonElement> result) {
		try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			JsonElement element = GSON.fromJson(reader, JsonElement.class);
			if (element != null) {
				result.put(new ResourceLocation(namespace, name), element);
			}
		} catch (Exception exception) {
			NpcDirector.LOGGER.warn("JSON invalido {}:{}: {}", namespace, name, exception.getMessage());
		}
	}
}
