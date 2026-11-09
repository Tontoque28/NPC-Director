package com.tontoque28.npcdirector.data;

import java.util.UUID;

import net.minecraft.nbt.CompoundNBT;

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

	public void write(CompoundNBT output) {
		output.putString("NpcId", npcId.toString());
		output.putString("DisplayName", displayName);
		output.putString("QuestRelevance", questRelevance.name());
		output.putBoolean("TargetableByHostiles", targetableByHostiles);
		output.putBoolean("RespawnOnEventReset", respawnOnEventReset);
		output.putBoolean("PersistentNoDespawn", persistentNoDespawn);
		output.putString("AssignedPathId", assignedPathId);
		output.putString("AssignedDialogueId", assignedDialogueId);
	}

	public static NpcProfile read(CompoundNBT input) {
		NpcProfile profile = new NpcProfile();
		String id = input.getString("NpcId");
		if (!id.isEmpty()) {
			try {
				profile.npcId = UUID.fromString(id);
			} catch (IllegalArgumentException exception) {
				profile.npcId = UUID.randomUUID();
			}
		}
		profile.displayName = input.getString("DisplayName");
		try {
			profile.questRelevance = QuestRelevance.valueOf(input.contains("QuestRelevance") ? input.getString("QuestRelevance") : "NONE");
		} catch (IllegalArgumentException exception) {
			profile.questRelevance = QuestRelevance.NONE;
		}
		profile.targetableByHostiles = input.contains("TargetableByHostiles") && input.getBoolean("TargetableByHostiles");
		profile.respawnOnEventReset = !input.contains("RespawnOnEventReset") || input.getBoolean("RespawnOnEventReset");
		profile.persistentNoDespawn = !input.contains("PersistentNoDespawn") || input.getBoolean("PersistentNoDespawn");
		profile.assignedPathId = input.getString("AssignedPathId");
		profile.assignedDialogueId = input.getString("AssignedDialogueId");
		return profile;
	}
}
