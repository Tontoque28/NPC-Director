package com.tontoque28.npcdirector.world;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tontoque28.npcdirector.NpcDirector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class WorldContent extends SavedData {

	public static final String NAMESPACE = "mundo";
	public static final int MAX_NAME = 32;
	public static final int MAX_ENTRIES = 256;
	private static final Codec<Map<String, String>> STRINGS = Codec.unboundedMap(Codec.STRING, Codec.STRING);
	private static final Codec<WorldContent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			STRINGS.optionalFieldOf("Paths", Map.of()).forGetter(content -> content.paths),
			STRINGS.optionalFieldOf("Dialogues", Map.of()).forGetter(content -> content.dialogues)
	).apply(instance, WorldContent::new));
	private static final SavedDataType<WorldContent> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(NpcDirector.MOD_ID, "world"),
			WorldContent::new,
			CODEC,
			DataFixTypes.LEVEL
	);
	private static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,32}");

	private final Map<String, String> paths = new TreeMap<>();
	private final Map<String, String> dialogues = new TreeMap<>();

	public static boolean validName(String name) {
		return name != null && NAME.matcher(name).matches();
	}

	public WorldContent() {
	}

	private WorldContent(Map<String, String> pathMap, Map<String, String> dialogueMap) {
		pathMap.forEach((key, value) -> {
			if (validName(key)) {
				paths.put(key, value);
			}
		});
		dialogueMap.forEach((key, value) -> {
			if (validName(key)) {
				dialogues.put(key, value);
			}
		});
	}

	public static WorldContent get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean putPath(String name, String json) {
		if (!paths.containsKey(name) && paths.size() >= MAX_ENTRIES) {
			return false;
		}
		paths.put(name, json);
		setDirty();
		publish();
		return true;
	}

	public boolean putDialogue(String name, String json) {
		if (!dialogues.containsKey(name) && dialogues.size() >= MAX_ENTRIES) {
			return false;
		}
		dialogues.put(name, json);
		setDirty();
		publish();
		return true;
	}

	public boolean removePath(String name) {
		boolean removed = paths.remove(name) != null;
		if (removed) {
			setDirty();
			publish();
		}
		return removed;
	}

	public boolean removeDialogue(String name) {
		boolean removed = dialogues.remove(name) != null;
		if (removed) {
			setDirty();
			publish();
		}
		return removed;
	}

	public String path(String name) {
		return paths.get(name);
	}

	public String dialogue(String name) {
		return dialogues.get(name);
	}

	public int pathCount() {
		return paths.size();
	}

	public int dialogueCount() {
		return dialogues.size();
	}

	public void publish() {
		WaypointPathManager.setWorld(paths);
		DialogueManager.setWorld(dialogues);
	}

	public static void unpublish() {
		WaypointPathManager.setWorld(Map.of());
		DialogueManager.setWorld(Map.of());
	}
}
