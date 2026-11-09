package com.tontoque28.npcdirector.data;

import java.util.UUID;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NpcProfile {

	private UUID npcId = UUID.randomUUID();
	private String displayName = "";
	private QuestRelevance questRelevance = QuestRelevance.NONE;
	private boolean targetableByHostiles = false;
	private boolean respawnOnEventReset = true;
	private boolean persistentNoDespawn = true;
	private String assignedPathId = "";
	private String assignedDialogueId = "";

	public UUID getNpcId() {
		return npcId;
	}

	public void setNpcId(UUID npcId) {
		this.npcId = npcId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public QuestRelevance getQuestRelevance() {
		return questRelevance;
	}

	public void setQuestRelevance(QuestRelevance questRelevance) {
		this.questRelevance = questRelevance;
	}

	public boolean isTargetableByHostiles() {
		return targetableByHostiles;
	}

	public void setTargetableByHostiles(boolean targetableByHostiles) {
		this.targetableByHostiles = targetableByHostiles;
	}

	public boolean isRespawnOnEventReset() {
		return respawnOnEventReset;
	}

	public void setRespawnOnEventReset(boolean respawnOnEventReset) {
		this.respawnOnEventReset = respawnOnEventReset;
	}

	public boolean isPersistentNoDespawn() {
		return persistentNoDespawn;
	}

	public void setPersistentNoDespawn(boolean persistentNoDespawn) {
		this.persistentNoDespawn = persistentNoDespawn;
	}

	public String getAssignedPathId() {
		return assignedPathId;
	}

	public void setAssignedPathId(String assignedPathId) {
		this.assignedPathId = assignedPathId;
	}

	public String getAssignedDialogueId() {
		return assignedDialogueId;
	}

	public void setAssignedDialogueId(String assignedDialogueId) {
		this.assignedDialogueId = assignedDialogueId;
	}

	public void write(ValueOutput output) {
		output.putString("NpcId", npcId.toString());
		output.putString("DisplayName", displayName);
		output.putString("QuestRelevance", questRelevance.name());
		output.putBoolean("TargetableByHostiles", targetableByHostiles);
		output.putBoolean("RespawnOnEventReset", respawnOnEventReset);
		output.putBoolean("PersistentNoDespawn", persistentNoDespawn);
		output.putString("AssignedPathId", assignedPathId);
		output.putString("AssignedDialogueId", assignedDialogueId);
	}

	public static NpcProfile read(ValueInput input) {
		NpcProfile profile = new NpcProfile();
		String id = input.getStringOr("NpcId", "");
		if (!id.isEmpty()) {
			try {
				profile.npcId = UUID.fromString(id);
			} catch (IllegalArgumentException exception) {
				profile.npcId = UUID.randomUUID();
			}
		}
		profile.displayName = input.getStringOr("DisplayName", "");
		try {
			profile.questRelevance = QuestRelevance.valueOf(input.getStringOr("QuestRelevance", "NONE"));
		} catch (IllegalArgumentException exception) {
			profile.questRelevance = QuestRelevance.NONE;
		}
		profile.targetableByHostiles = input.getBooleanOr("TargetableByHostiles", false);
		profile.respawnOnEventReset = input.getBooleanOr("RespawnOnEventReset", true);
		profile.persistentNoDespawn = input.getBooleanOr("PersistentNoDespawn", true);
		profile.assignedPathId = input.getStringOr("AssignedPathId", "");
		profile.assignedDialogueId = input.getStringOr("AssignedDialogueId", "");
		return profile;
	}
}
