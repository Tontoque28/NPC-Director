package com.tontoque28.npcdirector.world;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class WorldContent extends SavedData {

	public static final String NAMESPACE = "mundo";
	public static final int MAX_NAME = 32;
	public static final int MAX_ENTRIES = 256;
	private static final String DATA_NAME = "npcdirector_world";
	private static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,32}");

	private final Map<String, String> paths = new TreeMap<>();
	private final Map<String, String> dialogues = new TreeMap<>();

	public static boolean validName(String name) {
		return name != null && NAME.matcher(name).matches();
	}

	public static WorldContent get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(WorldContent::new, DATA_NAME);
	}

	public WorldContent() {
		super(DATA_NAME);
	}

	@Override
	public void load(CompoundTag tag) {
		WorldContent content = this;
		CompoundTag pathTag = tag.getCompound("Paths");
		for (String key : pathTag.getAllKeys()) {
			if (validName(key)) {
				content.paths.put(key, pathTag.getString(key));
			}
		}
		CompoundTag dialogueTag = tag.getCompound("Dialogues");
		for (String key : dialogueTag.getAllKeys()) {
			if (validName(key)) {
				content.dialogues.put(key, dialogueTag.getString(key));
			}
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		CompoundTag pathTag = new CompoundTag();
		paths.forEach(pathTag::putString);
		tag.put("Paths", pathTag);
		CompoundTag dialogueTag = new CompoundTag();
		dialogues.forEach(dialogueTag::putString);
		tag.put("Dialogues", dialogueTag);
		return tag;
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
		WaypointPathManager.setWorld(java.util.Collections.emptyMap());
		DialogueManager.setWorld(java.util.Collections.emptyMap());
	}
}
