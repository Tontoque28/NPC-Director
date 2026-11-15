package com.tontoque28.npcdirector.dialogue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Dialogue {

	private final String id;
	private final Map<String, DialogueNode> nodes = new LinkedHashMap<>();

	public Dialogue(String id, List<DialogueNode> nodeList) {
		this.id = id;
		for (DialogueNode node : nodeList) {
			nodes.put(node.getId(), node);
		}
	}

	public String getId() {
		return id;
	}

	public DialogueNode getNode(String nodeId) {
		return nodes.get(nodeId);
	}

	public DialogueNode resolveEntryNode(QuestStateProvider provider) {
		for (DialogueNode node : nodes.values()) {
			if (node.isAvailable(provider)) {
				return node;
			}
		}
		return null;
	}
}
