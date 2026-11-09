package com.tontoque28.npcdirector.dialogue;

import java.util.List;

public class DialogueNode {

	private final String id;
	private final String text;
	private final DialogueCondition condition;
	private final List<DialogueOption> options;

	public DialogueNode(String id, String text, DialogueCondition condition, List<DialogueOption> options) {
		this.id = id;
		this.text = text;
		this.condition = condition;
		this.options = options;
	}

	public String getId() {
		return id;
	}

	public String getText() {
		return text;
	}

	public DialogueCondition getCondition() {
		return condition;
	}

	public List<DialogueOption> getOptions() {
		return options;
	}

	public boolean isAvailable(QuestStateProvider provider) {
		return condition == null || condition.matches(provider);
	}
}
