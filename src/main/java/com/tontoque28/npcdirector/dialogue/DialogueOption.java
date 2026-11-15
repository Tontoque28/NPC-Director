package com.tontoque28.npcdirector.dialogue;

import java.util.List;

public class DialogueOption {

	private final String responseText;
	private final String targetNodeId;
	private final List<String> commands;
	private final String setQuestId;
	private final String setQuestState;

	public DialogueOption(String responseText, String targetNodeId, List<String> commands, String setQuestId, String setQuestState) {
		this.responseText = responseText;
		this.targetNodeId = targetNodeId;
		this.commands = commands;
		this.setQuestId = setQuestId;
		this.setQuestState = setQuestState;
	}

	public String getResponseText() {
		return responseText;
	}

	public String getTargetNodeId() {
		return targetNodeId;
	}

	public String getNextNodeId() {
		return targetNodeId;
	}

	public List<String> getCommands() {
		return commands;
	}

	public List<String> getCommandsToRun() {
		return commands;
	}

	public String getSetQuestId() {
		return setQuestId;
	}

	public String getSetQuestState() {
		return setQuestState;
	}

	public boolean changesQuestState() {
		return setQuestId != null && !setQuestId.isEmpty();
	}
}
