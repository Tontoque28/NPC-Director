package com.tontoque28.npcdirector.admin;

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

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AdminService {

	public static final double MAX_DISTANCE = 8.0;
	public static final double LOOK_DISTANCE = 6.0;

	public static QuestNpcEntity validate(ServerPlayer player, int entityId, String action) {
		if (!player.hasPermissions(2)) {
			DebugLog.denied("admin " + action + " denegado por permisos: " + player.getName().getString());
			return null;
		}
		Entity entity = player.level().getEntity(entityId);
		if (!(entity instanceof QuestNpcEntity npc)) {
			DebugLog.denied("admin " + action + " con entidad invalida " + entityId + " de " + player.getName().getString());
			return null;
		}
		if (player.distanceTo(npc) > MAX_DISTANCE) {
			DebugLog.denied("admin " + action + " fuera de distancia de " + player.getName().getString());
			return null;
		}
		return npc;
	}

	public static void openFor(ServerPlayer player, QuestNpcEntity npc) {
		NpcProfile profile = npc.getProfile();
		NetworkingInit.sendToPlayer(player, new OpenAdminPayload(npc.getId(), profile.getDisplayName(),
				profile.getQuestRelevance().ordinal(), profile.isTargetableByHostiles(),
				profile.getAssignedPathId(), profile.getAssignedDialogueId(),
				WaypointPathManager.ids(), DialogueManager.ids()));
	}

	public static void openSummonPanel(ServerPlayer player) {
		NetworkingInit.sendToPlayer(player, new OpenAdminPayload(-1, "", 0, false, "", "",
				WaypointPathManager.ids(), DialogueManager.ids()));
	}

	public static QuestNpcEntity lookedAt(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 view = player.getViewVector(1.0F).scale(LOOK_DISTANCE);
		Vec3 end = eye.add(view);
		AABB box = player.getBoundingBox().expandTowards(view).inflate(1.0);
		QuestNpcEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (QuestNpcEntity npc : player.level().getEntitiesOfClass(QuestNpcEntity.class, box)) {
			java.util.Optional<Vec3> hit = npc.getBoundingBox().inflate(0.3).clip(eye, end);
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

	public static void apply(ServerPlayer player, ApplyAdminPayload payload) {
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
			player.sendSystemMessage(Component.translatable("npcdirector.gui.rejected"));
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
		player.sendSystemMessage(Component.translatable("npcdirector.gui.applied"));
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

	public static void delete(ServerPlayer player, int entityId) {
		QuestNpcEntity npc = validate(player, entityId, "eliminar");
		if (npc == null) {
			return;
		}
		npc.discard();
		player.sendSystemMessage(Component.translatable("npcdirector.gui.deleted"));
		DebugLog.action("admin eliminado npc por " + player.getName().getString());
	}

	public static void summon(ServerPlayer player) {
		if (!player.hasPermissions(2)) {
			DebugLog.denied("admin invocar denegado por permisos: " + player.getName().getString());
			return;
		}
		ServerLevel level = (ServerLevel) player.level();
		QuestNpcEntity npc = ModEntities.QUEST_NPC.get().create(level, EntitySpawnReason.COMMAND);
		if (npc == null) {
			return;
		}
		npc.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
		npc.finalizeSpawn(level, level.getCurrentDifficultyAt(player.blockPosition()), EntitySpawnReason.COMMAND, null);
		level.addFreshEntity(npc);
		player.sendSystemMessage(Component.translatable("npcdirector.gui.summoned"));
		DebugLog.action("admin invocado npc por " + player.getName().getString());
		openFor(player, npc);
	}
}
