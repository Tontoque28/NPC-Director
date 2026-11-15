package com.tontoque28.npcdirector.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

	private record PlayerEntry(String id, Map<String, String> states) {
		static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("Id").forGetter(PlayerEntry::id),
				STATES.optionalFieldOf("States", Map.of()).forGetter(PlayerEntry::states)
		).apply(instance, PlayerEntry::new));
	}

	private static final Codec<QuestManager> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			STATES.optionalFieldOf("Global", Map.of()).forGetter(manager -> manager.globalStates),
			PlayerEntry.CODEC.listOf().optionalFieldOf("Players", List.of()).forGetter(QuestManager::entries)
	).apply(instance, QuestManager::fromData));

	private static final SavedDataType<QuestManager> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(NpcDirector.MOD_ID, "quests"),
			QuestManager::new,
			CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<String, String> globalStates = new HashMap<>();
	private final Map<UUID, Map<String, String>> playerStates = new HashMap<>();

	public static QuestManager get(ServerLevel world) {
		return world.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	private static QuestManager fromData(Map<String, String> global, List<PlayerEntry> players) {
		QuestManager manager = new QuestManager();
		manager.globalStates.putAll(global);
		for (PlayerEntry entry : players) {
			try {
				manager.playerStates.put(UUID.fromString(entry.id()), new HashMap<>(entry.states()));
			} catch (IllegalArgumentException exception) {
			}
		}
		return manager;
	}

	private List<PlayerEntry> entries() {
		List<PlayerEntry> list = new ArrayList<>();
		playerStates.forEach((id, states) -> list.add(new PlayerEntry(id.toString(), new HashMap<>(states))));
		return list;
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
