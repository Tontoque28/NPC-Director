package com.tontoque28.npcdirector.dialogue;

public class DialogueCondition {

	private final String questId;
	private final String requiredState;

	public DialogueCondition(String questId, String requiredState) {
		this.questId = questId;
		this.requiredState = requiredState;
	}

	public String getQuestId() {
		return questId;
	}

	public String getRequiredState() {
		return requiredState;
	}

	public boolean matches(QuestStateProvider provider) {
		if (questId == null || questId.isEmpty()) {
			return true;
		}
		String current = provider.getQuestState(questId);
		return requiredState.equals(current);
	}
}
