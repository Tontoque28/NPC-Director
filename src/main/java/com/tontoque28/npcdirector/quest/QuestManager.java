package com.tontoque28.npcdirector.quest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.tontoque28.npcdirector.dialogue.QuestStateProvider;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;

public class QuestManager extends WorldSavedData {

	private static final String NONE = "none";
	private static final String DATA_NAME = "npcdirector_quests";
	private final Map<String, String> globalStates = new HashMap<>();
	private final Map<UUID, Map<String, String>> playerStates = new HashMap<>();

	public static QuestManager get(ServerWorld world) {
		ServerWorld overworld = world.getServer().overworld();
		return overworld.getDataStorage().computeIfAbsent(QuestManager::new, DATA_NAME);
	}

	public QuestManager() {
		super(DATA_NAME);
	}

	@Override
	public void load(CompoundNBT tag) {
		QuestManager manager = this;
		CompoundNBT global = tag.getCompound("Global");
		for (String key : global.getAllKeys()) {
			manager.globalStates.put(key, global.getString(key));
		}
		ListNBT players = tag.getList("Players", 10);
		for (int i = 0; i < players.size(); i++) {
			CompoundNBT entry = players.getCompound(i);
			try {
				UUID id = UUID.fromString(entry.getString("Id"));
				CompoundNBT states = entry.getCompound("States");
				Map<String, String> map = new HashMap<>();
				for (String key : states.getAllKeys()) {
					map.put(key, states.getString(key));
				}
				manager.playerStates.put(id, map);
			} catch (IllegalArgumentException exception) {
			}
		}
	}

	@Override
	public CompoundNBT save(CompoundNBT tag) {
		CompoundNBT global = new CompoundNBT();
		globalStates.forEach(global::putString);
		tag.put("Global", global);
		ListNBT players = new ListNBT();
		playerStates.forEach((id, states) -> {
			CompoundNBT entry = new CompoundNBT();
			entry.putString("Id", id.toString());
			CompoundNBT compound = new CompoundNBT();
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
