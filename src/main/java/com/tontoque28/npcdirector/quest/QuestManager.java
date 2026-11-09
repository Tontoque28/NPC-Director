package com.tontoque28.npcdirector.quest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.tontoque28.npcdirector.dialogue.QuestStateProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class QuestManager extends SavedData {

	private static final String NONE = "none";
	private static final String DATA_NAME = "npcdirector_quests";
	private final Map<String, String> globalStates = new HashMap<>();
	private final Map<UUID, Map<String, String>> playerStates = new HashMap<>();

	public static QuestManager get(ServerLevel world) {
		ServerLevel overworld = world.getServer().overworld();
		return overworld.getDataStorage().computeIfAbsent(QuestManager::load, QuestManager::new, DATA_NAME);
	}

	public static QuestManager load(CompoundTag tag) {
		QuestManager manager = new QuestManager();
		CompoundTag global = tag.getCompound("Global");
		for (String key : global.getAllKeys()) {
			manager.globalStates.put(key, global.getString(key));
		}
		ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
		for (int i = 0; i < players.size(); i++) {
			CompoundTag entry = players.getCompound(i);
			try {
				UUID id = UUID.fromString(entry.getString("Id"));
				CompoundTag states = entry.getCompound("States");
				Map<String, String> map = new HashMap<>();
				for (String key : states.getAllKeys()) {
					map.put(key, states.getString(key));
				}
				manager.playerStates.put(id, map);
			} catch (IllegalArgumentException exception) {
			}
		}
		return manager;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		CompoundTag global = new CompoundTag();
		globalStates.forEach(global::putString);
		tag.put("Global", global);
		ListTag players = new ListTag();
		playerStates.forEach((id, states) -> {
			CompoundTag entry = new CompoundTag();
			entry.putString("Id", id.toString());
			CompoundTag compound = new CompoundTag();
			states.forEach(compound::putString);
			entry.put("States", compound);
			players.add(entry);
		});
		tag.put("Players", players);
		return tag;
	}

	public String getGlobalState(String questId) {
		return globalStates.getOrDefault(questId, NONE);
	}

	public void setGlobalState(String questId, String state) {
		globalStates.put(questId, state);
		setDirty();
	}

	public String getPlayerState(UUID playerId, String questId) {
		Map<String, String> states = playerStates.get(playerId);
		if (states == null) {
			return NONE;
		}
		return states.getOrDefault(questId, NONE);
	}

	public void setPlayerState(UUID playerId, String questId, String state) {
		playerStates.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(questId, state);
		setDirty();
	}

	public QuestStateProvider providerFor(UUID playerId) {
		return questId -> {
			String playerState = getPlayerState(playerId, questId);
			if (!NONE.equals(playerState)) {
				return playerState;
			}
			return getGlobalState(questId);
		};
	}
}
