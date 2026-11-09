package com.tontoque28.npcdirector.admin;

import java.util.Optional;

import com.tontoque28.npcdirector.data.NpcProfile;
import com.tontoque28.npcdirector.data.QuestRelevance;
import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.entity.ModEntities;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.network.ApplyAdminPayload;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.util.DebugLog;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;

public class AdminService {

	public static final double MAX_DISTANCE = 8.0;
	public static final double LOOK_DISTANCE = 6.0;

	private static void tell(ServerPlayerEntity player, String key) {
		player.sendMessage(new TranslationTextComponent(key), Util.NIL_UUID);
	}

	public static QuestNpcEntity validate(ServerPlayerEntity player, int entityId, String action) {
		if (!player.hasPermissions(2)) {
			DebugLog.denied("admin " + action + " denegado por permisos: " + player.getName().getString());
			return null;
		}
		Entity entity = player.level.getEntity(entityId);
		if (!(entity instanceof QuestNpcEntity)) {
			DebugLog.denied("admin " + action + " con entidad invalida " + entityId + " de " + player.getName().getString());
			return null;
		}
		QuestNpcEntity npc = (QuestNpcEntity) entity;
		if (player.distanceTo(npc) > MAX_DISTANCE) {
			DebugLog.denied("admin " + action + " fuera de distancia de " + player.getName().getString());
			return null;
		}
		return npc;
	}

	public static void openFor(ServerPlayerEntity player, QuestNpcEntity npc) {
		NpcProfile profile = npc.getProfile();
		NetworkingInit.sendToPlayer(player, new OpenAdminPayload(npc.getId(), profile.getDisplayName(),
				profile.getQuestRelevance().ordinal(), profile.isTargetableByHostiles(),
				profile.getAssignedPathId(), profile.getAssignedDialogueId(),
				WaypointPathManager.ids(), DialogueManager.ids()));
	}

	public static void openSummonPanel(ServerPlayerEntity player) {
		NetworkingInit.sendToPlayer(player, new OpenAdminPayload(-1, "", 0, false, "", "",
				WaypointPathManager.ids(), DialogueManager.ids()));
	}

	public static QuestNpcEntity lookedAt(ServerPlayerEntity player) {
		Vector3d eye = player.getEyePosition(1.0F);
		Vector3d view = player.getViewVector(1.0F).scale(LOOK_DISTANCE);
		Vector3d end = eye.add(view);
		AxisAlignedBB box = player.getBoundingBox().expandTowards(view).inflate(1.0);
		QuestNpcEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (QuestNpcEntity npc : player.level.getEntitiesOfClass(QuestNpcEntity.class, box)) {
			Optional<Vector3d> hit = npc.getBoundingBox().inflate(0.3).clip(eye, end);
			if (hit.isPresent()) {
				double distance = eye.distanceToSqr(hit.get());
				if (distance < bestDistance) {
					bestDistance = distance;
					best = npc;
				}
			}
		}
		return best;
	}

	public static void apply(ServerPlayerEntity player, ApplyAdminPayload payload) {
		QuestNpcEntity npc = validate(player, payload.npcEntityId(), "aplicar");
		if (npc == null) {
			return;
		}
		NpcProfile profile = npc.getProfile();
		String pathId = payload.pathId();
		String dialogueId = payload.dialogueId();
		boolean pathOk = pathId.isEmpty() || pathId.equals(profile.getAssignedPathId()) || WaypointPathManager.ids().contains(pathId);
		boolean dialogueOk = dialogueId.isEmpty() || dialogueId.equals(profile.getAssignedDialogueId()) || DialogueManager.ids().contains(dialogueId);
		if (!pathOk || !dialogueOk) {
			DebugLog.denied("admin aplicar con camino o dialogo desconocido de " + player.getName().getString());
			tell(player, "npcdirector.gui.rejected");
			return;
		}
		profile.setDisplayName(sanitizeName(payload.name()));
		QuestRelevance[] values = QuestRelevance.values();
		if (payload.relevance() >= 0 && payload.relevance() < values.length) {
			profile.setQuestRelevance(values[payload.relevance()]);
		}
		profile.setTargetableByHostiles(payload.targetable());
		if (!payload.pathId().equals(profile.getAssignedPathId())) {
			profile.setAssignedPathId(payload.pathId());
			npc.setPatrolIndex(0);
			npc.setPatrolIdleTicks(-1);
		}
		profile.setAssignedDialogueId(payload.dialogueId());
		npc.setProfile(profile);
		tell(player, "npcdirector.gui.applied");
		DebugLog.action("admin aplicado por " + player.getName().getString() + " npc=" + npc.getId());
	}

	public static String sanitizeName(String raw) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < raw.length() && builder.length() < 64; i++) {
			char c = raw.charAt(i);
			if (c >= 32 && c != 127 && c != '\u00a7') {
				builder.append(c);
			}
		}
		return builder.toString().trim();
	}

	public static void delete(ServerPlayerEntity player, int entityId) {
		QuestNpcEntity npc = validate(player, entityId, "eliminar");
		if (npc == null) {
			return;
		}
		npc.remove();
		tell(player, "npcdirector.gui.deleted");
		DebugLog.action("admin eliminado npc por " + player.getName().getString());
	}

	public static void summon(ServerPlayerEntity player) {
		if (!player.hasPermissions(2)) {
			DebugLog.denied("admin invocar denegado por permisos: " + player.getName().getString());
			return;
		}
		ServerWorld level = (ServerWorld) player.level;
		QuestNpcEntity npc = ModEntities.QUEST_NPC.get().create(level);
		if (npc == null) {
			return;
		}
		npc.moveTo(player.getX(), player.getY(), player.getZ(), player.yRot, 0.0F);
		npc.finalizeSpawn(level, level.getCurrentDifficultyAt(player.blockPosition()), SpawnReason.COMMAND, null, null);
		level.addFreshEntity(npc);
		tell(player, "npcdirector.gui.summoned");
		DebugLog.action("admin invocado npc por " + player.getName().getString());
		openFor(player, npc);
	}
}
