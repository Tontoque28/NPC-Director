package com.tontoque28.npcdirector.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.util.DataLoader;
import com.tontoque28.npcdirector.util.Ids;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DialogueManager {

	private static final Map<ResourceLocation, Dialogue> DIALOGUES = new HashMap<>();

	public static void reload() {
		loadFromJson(DataLoader.load("dialogues"));
	}

	public static java.util.List<String> ids() {
		return DIALOGUES.keySet().stream().map(ResourceLocation::toString).sorted().collect(java.util.stream.Collectors.toList());
	}

	public static Dialogue get(ResourceLocation id) {
		return DIALOGUES.get(id);
	}

	public static Dialogue get(String id) {
		ResourceLocation parsed = Ids.parse(id);
		return parsed == null ? null : DIALOGUES.get(parsed);
	}

	public static void loadFromJson(Map<ResourceLocation, JsonElement> prepared) {
		DIALOGUES.clear();
		for (Map.Entry<ResourceLocation, JsonElement> entry : prepared.entrySet()) {
			try {
				Dialogue dialogue = parseDialogue(entry.getKey(), entry.getValue().getAsJsonObject());
				DIALOGUES.put(entry.getKey(), dialogue);
			} catch (Exception exception) {
				NpcDirector.LOGGER.warn("Dialogo invalido {}: {}", entry.getKey(), exception.getMessage());
			}
		}
		NpcDirector.LOGGER.info("Dialogos cargados: {}", DIALOGUES.size());
	}

	private static Dialogue parseDialogue(ResourceLocation id, JsonObject root) {
		List<DialogueNode> nodes = new ArrayList<>();
		JsonArray nodesArray = root.getAsJsonArray("nodes");
		for (JsonElement nodeElement : nodesArray) {
			nodes.add(parseNode(nodeElement.getAsJsonObject()));
		}
		return new Dialogue(id.toString(), nodes);
	}

	private static DialogueNode parseNode(JsonObject nodeObject) {
		String nodeId = nodeObject.get("id").getAsString();
		String text = nodeObject.get("text").getAsString();
		DialogueCondition condition = null;
		if (nodeObject.has("requires")) {
			JsonObject req = nodeObject.getAsJsonObject("requires");
			condition = new DialogueCondition(req.get("quest").getAsString(), req.get("state").getAsString());
		}
		List<DialogueOption> options = new ArrayList<>();
		if (nodeObject.has("options")) {
			for (JsonElement optionElement : nodeObject.getAsJsonArray("options")) {
				options.add(parseOption(optionElement.getAsJsonObject()));
			}
		}
		return new DialogueNode(nodeId, text, condition, options);
	}

	private static DialogueOption parseOption(JsonObject optionObject) {
		String responseText = optionObject.get("text").getAsString();
		String targetNodeId = optionObject.has("next") ? optionObject.get("next").getAsString() : null;
		List<String> commands = new ArrayList<>();
		if (optionObject.has("commands")) {
			for (JsonElement commandElement : optionObject.getAsJsonArray("commands")) {
				commands.add(commandElement.getAsString());
			}
		}
		String setQuestId = null;
		String setQuestState = null;
		if (optionObject.has("setQuest")) {
			JsonObject setQuest = optionObject.getAsJsonObject("setQuest");
			setQuestId = setQuest.get("quest").getAsString();
			setQuestState = setQuest.get("state").getAsString();
		}
		return new DialogueOption(responseText, targetNodeId, commands, setQuestId, setQuestState);
	}
}
