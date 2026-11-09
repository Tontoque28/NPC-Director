package com.tontoque28.npcdirector.quest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.tontoque28.npcdirector.dialogue.QuestStateProvider;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

public class QuestManager extends WorldSavedData {

	private static final String NONE = "none";
	private static final String DATA_NAME = "npcdirector_quests";
	private final Map<String, String> globalStates = new HashMap<>();
	private final Map<UUID, Map<String, String>> playerStates = new HashMap<>();

	public QuestManager() {
		super(DATA_NAME);
	}

	public QuestManager(String name) {
		super(name);
	}

	public static QuestManager get(WorldServer world) {
		World overworld = world.getMinecraftServer().getWorld(0);
		MapStorage storage = overworld.getMapStorage();
		QuestManager manager = (QuestManager) storage.getOrLoadData(QuestManager.class, DATA_NAME);
		if (manager == null) {
			manager = new QuestManager();
			storage.setData(DATA_NAME, manager);
		}
		return manager;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		globalStates.clear();
		playerStates.clear();
		NBTTagCompound global = tag.getCompoundTag("Global");
		for (String key : global.getKeySet()) {
			globalStates.put(key, global.getString(key));
		}
		NBTTagList players = tag.getTagList("Players", 10);
		for (int i = 0; i < players.tagCount(); i++) {
			NBTTagCompound entry = players.getCompoundTagAt(i);
			try {
				UUID id = UUID.fromString(entry.getString("Id"));
				NBTTagCompound states = entry.getCompoundTag("States");
				Map<String, String> map = new HashMap<>();
				for (String key : states.getKeySet()) {
					map.put(key, states.getString(key));
				}
				playerStates.put(id, map);
			} catch (IllegalArgumentException exception) {
			}
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		NBTTagCompound global = new NBTTagCompound();
		for (Map.Entry<String, String> entry : globalStates.entrySet()) {
			global.setString(entry.getKey(), entry.getValue());
		}
		tag.setTag("Global", global);
		NBTTagList players = new NBTTagList();
		for (Map.Entry<UUID, Map<String, String>> playerEntry : playerStates.entrySet()) {
			NBTTagCompound entry = new NBTTagCompound();
			entry.setString("Id", playerEntry.getKey().toString());
			NBTTagCompound compound = new NBTTagCompound();
			for (Map.Entry<String, String> state : playerEntry.getValue().entrySet()) {
				compound.setString(state.getKey(), state.getValue());
			}
			entry.setTag("States", compound);
			players.appendTag(entry);
		}
		tag.setTag("Players", players);
		return tag;
	}

	public String getGlobalState(String questId) {
		return globalStates.getOrDefault(questId, NONE);
	}

	public void setGlobalState(String questId, String state) {
		globalStates.put(questId, state);
		markDirty();
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
		markDirty();
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
