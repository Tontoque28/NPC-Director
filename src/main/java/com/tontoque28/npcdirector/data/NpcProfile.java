package com.tontoque28.npcdirector.data;

import java.util.UUID;

import com.tontoque28.npcdirector.util.Validate;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NpcProfile {

	public static final int MAX_PNG_BASE64 = 22000;

	private UUID npcId = UUID.randomUUID();
	private String displayName = "";
	private QuestRelevance questRelevance = QuestRelevance.NONE;
	private boolean targetableByHostiles = false;
	private boolean respawnOnEventReset = true;
	private boolean persistentNoDespawn = true;
	private String assignedPathId = "";
	private String assignedDialogueId = "";
	private String skinName = "";
	private String skinTexture = "";
	private String skinUuid = "";
	private String skinPng = "";
	private String skinPngHash = "";
	private float scale = 1.0F;
	private int armModel = 0;
	private boolean nameVisible = true;
	private int nameColor = 15;
	private boolean lookAtPlayer = true;
	private boolean stationary = false;
	private boolean invulnerable = false;
	private float patrolSpeed = 1.0F;
	private boolean sitting = false;
	private String greeting = "";
	private int greetRange = 5;
	private String soundId = "";
	private float soundVolume = 1.0F;
	private float soundPitch = 1.0F;
	private boolean enabled = true;

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

	public String getSkinName() {
		return skinName;
	}

	public void setSkinName(String skinName) {
		this.skinName = skinName;
	}

	public String getSkinTexture() {
		return skinTexture;
	}

	public void setSkinTexture(String skinTexture) {
		this.skinTexture = skinTexture;
	}

	public String getSkinUuid() {
		return skinUuid;
	}

	public void setSkinUuid(String skinUuid) {
		this.skinUuid = skinUuid;
	}

	public String getSkinPng() {
		return skinPng;
	}

	public String getSkinPngHash() {
		return skinPngHash;
	}

	public void setSkinPng(String encoded) {
		String value = encoded == null ? "" : encoded;
		if (value.isEmpty()) {
			this.skinPng = "";
			this.skinPngHash = "";
			return;
		}
		try {
			this.skinPngHash = com.tontoque28.npcdirector.skin.SkinPng.hash(java.util.Base64.getDecoder().decode(value));
			this.skinPng = value;
		} catch (IllegalArgumentException exception) {
			this.skinPng = "";
			this.skinPngHash = "";
		}
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public int getArmModel() {
		return armModel;
	}

	public void setArmModel(int armModel) {
		this.armModel = armModel;
	}

	public boolean isNameVisible() {
		return nameVisible;
	}

	public void setNameVisible(boolean nameVisible) {
		this.nameVisible = nameVisible;
	}

	public int getNameColor() {
		return nameColor;
	}

	public void setNameColor(int nameColor) {
		this.nameColor = nameColor;
	}

	public boolean isLookAtPlayer() {
		return lookAtPlayer;
	}

	public void setLookAtPlayer(boolean lookAtPlayer) {
		this.lookAtPlayer = lookAtPlayer;
	}

	public boolean isStationary() {
		return stationary;
	}

	public void setStationary(boolean stationary) {
		this.stationary = stationary;
	}

	public boolean isInvulnerable() {
		return invulnerable;
	}

	public void setInvulnerable(boolean invulnerable) {
		this.invulnerable = invulnerable;
	}

	public float getPatrolSpeed() {
		return patrolSpeed;
	}

	public void setPatrolSpeed(float patrolSpeed) {
		this.patrolSpeed = patrolSpeed;
	}

	public boolean isSitting() {
		return sitting;
	}

	public void setSitting(boolean sitting) {
		this.sitting = sitting;
	}

	public String getGreeting() {
		return greeting;
	}

	public void setGreeting(String greeting) {
		this.greeting = greeting;
	}

	public int getGreetRange() {
		return greetRange;
	}

	public void setGreetRange(int greetRange) {
		this.greetRange = greetRange;
	}

	public String getSoundId() {
		return soundId;
	}

	public void setSoundId(String soundId) {
		this.soundId = soundId;
	}

	public float getSoundVolume() {
		return soundVolume;
	}

	public void setSoundVolume(float soundVolume) {
		this.soundVolume = soundVolume;
	}

	public float getSoundPitch() {
		return soundPitch;
	}

	public void setSoundPitch(float soundPitch) {
		this.soundPitch = soundPitch;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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
		output.putString("SkinName", skinName);
		output.putString("SkinTexture", skinTexture);
		output.putString("SkinUuid", skinUuid);
		output.putString("SkinPng", skinPng);
		output.putString("SkinPngHash", skinPngHash);
		output.putFloat("Scale", scale);
		output.putInt("ArmModel", armModel);
		output.putBoolean("NameVisible", nameVisible);
		output.putInt("NameColor", nameColor);
		output.putBoolean("LookAtPlayer", lookAtPlayer);
		output.putBoolean("Stationary", stationary);
		output.putBoolean("Invulnerable", invulnerable);
		output.putFloat("PatrolSpeed", patrolSpeed);
		output.putBoolean("Sitting", sitting);
		output.putString("Greeting", greeting);
		output.putInt("GreetRange", greetRange);
		output.putString("SoundId", soundId);
		output.putFloat("SoundVolume", soundVolume);
		output.putFloat("SoundPitch", soundPitch);
		output.putBoolean("Enabled", enabled);
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
		profile.displayName = Validate.cleanText(input.getStringOr("DisplayName", ""), 64);
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
		String skin = input.getStringOr("SkinName", "");
		profile.skinName = Validate.validSkinName(skin) ? skin : "";
		String texture = input.getStringOr("SkinTexture", "");
		profile.skinTexture = texture.length() <= 16384 && texture.matches("[A-Za-z0-9+/=]*") ? texture : "";
		String skinUuid = input.getStringOr("SkinUuid", "");
		profile.skinUuid = skinUuid.length() <= 36 ? skinUuid : "";
		String skinPng = input.getStringOr("SkinPng", "");
		profile.setSkinPng(skinPng.length() <= MAX_PNG_BASE64 && skinPng.matches("[A-Za-z0-9+/=]*") ? skinPng : "");
		profile.scale = Validate.clampFloat(input.getFloatOr("Scale", 1.0F), Validate.SCALE_MIN, Validate.SCALE_MAX, 1.0F);
		profile.armModel = Math.max(0, Math.min(2, input.getIntOr("ArmModel", 0)));
		profile.nameVisible = input.getBooleanOr("NameVisible", true);
		profile.nameColor = Math.max(0, Math.min(15, input.getIntOr("NameColor", 15)));
		profile.lookAtPlayer = input.getBooleanOr("LookAtPlayer", true);
		profile.stationary = input.getBooleanOr("Stationary", false);
		profile.invulnerable = input.getBooleanOr("Invulnerable", false);
		profile.patrolSpeed = Validate.clampFloat(input.getFloatOr("PatrolSpeed", 1.0F), Validate.SPEED_MIN, Validate.SPEED_MAX, 1.0F);
		profile.sitting = input.getBooleanOr("Sitting", false);
		profile.greeting = Validate.cleanText(input.getStringOr("Greeting", ""), Validate.GREETING_MAX);
		profile.greetRange = Math.max(1, Math.min(16, input.getIntOr("GreetRange", 5)));
		String soundId = input.getStringOr("SoundId", "");
		profile.soundId = soundId.length() <= Validate.ID_MAX ? soundId : "";
		profile.soundVolume = Validate.clampFloat(input.getFloatOr("SoundVolume", 1.0F), 0.0F, 2.0F, 1.0F);
		profile.soundPitch = Validate.clampFloat(input.getFloatOr("SoundPitch", 1.0F), 0.5F, 2.0F, 1.0F);
		profile.enabled = input.getBooleanOr("Enabled", true);
		return profile;
	}
}
