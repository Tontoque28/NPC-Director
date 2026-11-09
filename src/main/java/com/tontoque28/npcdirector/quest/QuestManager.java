package com.tontoque28.npcdirector.quest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.dialogue.QuestStateProvider;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class QuestManager extends SavedData {

	private static final String NONE = "none";
	private static final Codec<Map<String, String>> STATES = Codec.unboundedMap(Codec.STRING, Codec.STRING);
	private static final Codec<QuestManager> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			STATES.optionalFieldOf("Global", Map.of()).forGetter(manager -> manager.globalStates),
			Codec.unboundedMap(Codec.STRING, STATES).optionalFieldOf("Players", Map.of()).forGetter(QuestManager::playersAsStrings)
	).apply(instance, QuestManager::new));
	private static final SavedDataType<QuestManager> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(NpcDirector.MOD_ID, "quests"),
			QuestManager::new,
			CODEC,
			DataFixTypes.LEVEL
	);
	private final Map<String, String> globalStates = new HashMap<>();
	private final Map<UUID, Map<String, String>> playerStates = new HashMap<>();

	public QuestManager() {
	}

	private QuestManager(Map<String, String> global, Map<String, Map<String, String>> players) {
		globalStates.putAll(global);
		players.forEach((key, states) -> {
			try {
				playerStates.put(UUID.fromString(key), new HashMap<>(states));
			} catch (IllegalArgumentException exception) {
			}
		});
	}

	private Map<String, Map<String, String>> playersAsStrings() {
		Map<String, Map<String, String>> result = new HashMap<>();
		playerStates.forEach((id, states) -> result.put(id.toString(), states));
		return result;
	}

	public static QuestManager get(ServerLevel world) {
		return world.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
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
