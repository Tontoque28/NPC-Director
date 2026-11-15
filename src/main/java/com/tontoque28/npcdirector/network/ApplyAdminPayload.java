package com.tontoque28.npcdirector.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class ApplyAdminPayload implements IMessage {

	private int npcEntityId;
	private String name = "";
	private int relevance;
	private boolean targetable;
	private String pathId = "";
	private String dialogueId = "";
	private boolean lookAtPlayer = true;
	private boolean stationary;
	private boolean invulnerable;
	private boolean seated;
	private float patrolSpeed = 1.0F;

	public ApplyAdminPayload() {
	}

	public ApplyAdminPayload(int npcEntityId, String name, int relevance, boolean targetable, String pathId, String dialogueId,
			boolean lookAtPlayer, boolean stationary, boolean invulnerable, boolean seated, float patrolSpeed) {
		this.lookAtPlayer = lookAtPlayer;
		this.stationary = stationary;
		this.invulnerable = invulnerable;
		this.seated = seated;
		this.patrolSpeed = patrolSpeed;
		this.npcEntityId = npcEntityId;
		this.name = name;
		this.relevance = relevance;
		this.targetable = targetable;
		this.pathId = pathId;
		this.dialogueId = dialogueId;
	}

	public int npcEntityId() {
		return npcEntityId;
	}

	public String name() {
		return name;
	}

	public int relevance() {
		return relevance;
	}

	public boolean targetable() {
		return targetable;
	}

	public String pathId() {
		return pathId;
	}

	public String dialogueId() {
		return dialogueId;
	}

	public boolean lookAtPlayer() {
		return lookAtPlayer;
	}

	public boolean stationary() {
		return stationary;
	}

	public boolean invulnerable() {
		return invulnerable;
	}

	public boolean seated() {
		return seated;
	}

	public float patrolSpeed() {
		return patrolSpeed;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeVarInt(buf, npcEntityId, 5);
		ByteBufUtils.writeUTF8String(buf, cut(name, 64));
		ByteBufUtils.writeVarInt(buf, relevance, 5);
		buf.writeBoolean(targetable);
		ByteBufUtils.writeUTF8String(buf, cut(pathId, 128));
		ByteBufUtils.writeUTF8String(buf, cut(dialogueId, 128));
		buf.writeBoolean(lookAtPlayer);
		buf.writeBoolean(stationary);
		buf.writeBoolean(invulnerable);
		buf.writeBoolean(seated);
		buf.writeFloat(patrolSpeed);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		npcEntityId = ByteBufUtils.readVarInt(buf, 5);
		name = cut(ByteBufUtils.readUTF8String(buf), 64);
		relevance = ByteBufUtils.readVarInt(buf, 5);
		targetable = buf.readBoolean();
		pathId = cut(ByteBufUtils.readUTF8String(buf), 128);
		dialogueId = cut(ByteBufUtils.readUTF8String(buf), 128);
		lookAtPlayer = buf.readBoolean();
		stationary = buf.readBoolean();
		invulnerable = buf.readBoolean();
		seated = buf.readBoolean();
		patrolSpeed = buf.readFloat();
	}

	private static String cut(String text, int max) {
		return text.length() > max ? text.substring(0, max) : text;
	}
}
