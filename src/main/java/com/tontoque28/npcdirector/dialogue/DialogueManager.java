package com.tontoque28.npcdirector.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.util.Ids;
import com.tontoque28.npcdirector.util.JsonDataReloadListener;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DialogueManager {

	public static final int MAX_NODES = 64;
	public static final int MAX_OPTIONS = 64;
	public static final int MAX_TEXT = 8000;
	public static final int MAX_OPTION_TEXT = 512;
	public static final int MAX_COMMANDS = 32;
	public static final int MAX_COMMAND = 512;
	public static final int MAX_ID = 64;

	private static final Map<ResourceLocation, Dialogue> DIALOGUES = new HashMap<>();
	private static final Map<ResourceLocation, Dialogue> WORLD = new HashMap<>();

	public static List<String> ids() {
		return java.util.stream.Stream.concat(DIALOGUES.keySet().stream(), WORLD.keySet().stream()).map(ResourceLocation::toString).distinct().sorted().collect(java.util.stream.Collectors.toList());
	}

	public static void setWorld(Map<String, String> json) {
		WORLD.clear();
		for (Map.Entry<String, String> entry : json.entrySet()) {
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath(com.tontoque28.npcdirector.world.WorldContent.NAMESPACE, entry.getKey());
			try {
				WORLD.put(id, parseDialogue(id, com.google.gson.JsonParser.parseString(entry.getValue()).getAsJsonObject()));
			} catch (Exception exception) {
				NpcDirector.LOGGER.warn("Dialogo de mundo invalido {}: {}", id, exception.getMessage());
			}
		}
	}

	public static void register() {
		net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA).registerReloadListener(
				new JsonDataReloadListener(ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "dialogues"), "dialogues", DialogueManager::loadFromJson));
	}

	public static Dialogue get(ResourceLocation id) {
		Dialogue dialogue = DIALOGUES.get(id);
		return dialogue != null ? dialogue : WORLD.get(id);
	}

	public static Dialogue get(String id) {
		ResourceLocation parsed = Ids.parse(id);
		return parsed == null ? null : get(parsed);
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
			if (nodes.size() >= MAX_NODES) {
				break;
			}
			nodes.add(parseNode(nodeElement.getAsJsonObject()));
		}
		return new Dialogue(id.toString(), nodes);
	}

	private static DialogueNode parseNode(JsonObject nodeObject) {
		String nodeId = nodeObject.get("id").getAsString();
		String text = cut(nodeObject.get("text").getAsString(), MAX_TEXT);
		DialogueCondition condition = null;
		if (nodeObject.has("requires")) {
			JsonObject req = nodeObject.getAsJsonObject("requires");
			condition = new DialogueCondition(req.get("quest").getAsString(), req.get("state").getAsString());
		}
		List<DialogueOption> options = new ArrayList<>();
		if (nodeObject.has("options")) {
			for (JsonElement optionElement : nodeObject.getAsJsonArray("options")) {
				if (options.size() >= MAX_OPTIONS) {
					break;
				}
				options.add(parseOption(optionElement.getAsJsonObject()));
			}
		}
		return new DialogueNode(nodeId, text, condition, options);
	}

	private static DialogueOption parseOption(JsonObject optionObject) {
		String responseText = cut(optionObject.get("text").getAsString(), MAX_OPTION_TEXT);
		String targetNodeId = optionObject.has("next") ? optionObject.get("next").getAsString() : null;
		List<String> commands = new ArrayList<>();
		if (optionObject.has("commands")) {
			for (JsonElement commandElement : optionObject.getAsJsonArray("commands")) {
				if (commands.size() >= MAX_COMMANDS) {
					break;
				}
				commands.add(cut(commandElement.getAsString(), MAX_COMMAND));
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

	private static String cut(String text, int max) {
		return text.length() > max ? text.substring(0, max) : text;
	}
}
