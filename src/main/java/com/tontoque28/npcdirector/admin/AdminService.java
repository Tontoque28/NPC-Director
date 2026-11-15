package com.tontoque28.npcdirector.admin;

import java.util.List;

import com.tontoque28.npcdirector.config.ModConfig;
import com.tontoque28.npcdirector.data.NpcProfile;
import com.tontoque28.npcdirector.data.QuestRelevance;
import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.network.ApplyAdminPayload;
import com.tontoque28.npcdirector.network.AssignPathPayload;
import com.tontoque28.npcdirector.network.PanelMessagePayload;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.util.DebugLog;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

public class AdminService {

	public static final double MAX_DISTANCE = 8.0;
	public static final double LOOK_DISTANCE = 6.0;

	public static final int MAX_ASSIGN_RADIUS_NPCS = 32;
	public static final double ASSIGN_RADIUS = 16.0;

	public static boolean isOperator(EntityPlayerMP player) {
		return player.canUseCommand(2, "");
	}

	public static boolean canEdit(EntityPlayerMP player) {
		return isOperator(player) && ModConfig.isDevMode() && player.interactionManager.getGameType() == net.minecraft.world.GameType.CREATIVE;
	}

	public static void tell(EntityPlayerMP player, String key) {
		player.sendMessage(new TextComponentTranslation(key));
		NetworkingInit.sendToPlayer(player, new PanelMessagePayload(key));
	}

	public static QuestNpcEntity validate(EntityPlayerMP player, int entityId, String action) {
		if (!canEdit(player)) {
			DebugLog.denied("admin " + action + " denegado por permisos, modo jugador o no creativo: " + player.getName());
			return null;
		}
		Entity entity = player.world.getEntityByID(entityId);
		if (!(entity instanceof QuestNpcEntity)) {
			DebugLog.denied("admin " + action + " con entidad invalida " + entityId + " de " + player.getName());
			return null;
		}
		QuestNpcEntity npc = (QuestNpcEntity) entity;
		if (player.getDistance(npc) > MAX_DISTANCE) {
			DebugLog.denied("admin " + action + " fuera de distancia de " + player.getName());
			return null;
		}
		return npc;
	}

	public static void openFor(EntityPlayerMP player, QuestNpcEntity npc) {
		if (!canEdit(player)) {
			DebugLog.denied("panel de edicion denegado por permisos, modo jugador o no creativo: " + player.getName());
			return;
		}
		NpcProfile profile = npc.getProfile();
		NetworkingInit.sendToPlayer(player, new OpenAdminPayload(npc.getEntityId(), profile.getDisplayName(),
				profile.getQuestRelevance().ordinal(), profile.isTargetableByHostiles(),
				profile.getAssignedPathId(), profile.getAssignedDialogueId(),
				WaypointPathManager.ids(), DialogueManager.ids(),
				profile.isLookAtPlayer(), profile.isStationary(), profile.isInvulnerable(), profile.isSeated(),
				profile.getPatrolSpeed(), profile.getSkinPngHash()));
	}

	public static void openSummonPanel(EntityPlayerMP player) {
		if (!canEdit(player)) {
			DebugLog.denied("panel de invocacion denegado por permisos, modo jugador o no creativo: " + player.getName());
			return;
		}
		NetworkingInit.sendToPlayer(player, new OpenAdminPayload(-1, "", 0, false, "", "",
				WaypointPathManager.ids(), DialogueManager.ids(), true, false, false, false, 1.0F, ""));
	}

	public static QuestNpcEntity lookedAt(EntityPlayerMP player) {
		Vec3d eye = player.getPositionEyes(1.0F);
		Vec3d view = player.getLook(1.0F).scale(LOOK_DISTANCE);
		Vec3d end = eye.add(view);
		AxisAlignedBB box = player.getEntityBoundingBox().expand(view.x, view.y, view.z).grow(1.0);
		QuestNpcEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		List<QuestNpcEntity> candidates = player.world.getEntitiesWithinAABB(QuestNpcEntity.class, box);
		for (QuestNpcEntity npc : candidates) {
			RayTraceResult hit = npc.getEntityBoundingBox().grow(0.3).calculateIntercept(eye, end);
			if (hit != null) {
				double distance = eye.squareDistanceTo(hit.hitVec);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = npc;
				}
			}
		}
		return best;
	}

	public static void apply(EntityPlayerMP player, ApplyAdminPayload payload) {
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
			DebugLog.denied("admin aplicar con camino o dialogo desconocido de " + player.getName());
			tell(player, "npcdirector.gui.rejected");
			return;
		}
		profile.setDisplayName(sanitizeName(payload.name()));
		QuestRelevance[] values = QuestRelevance.values();
		if (payload.relevance() >= 0 && payload.relevance() < values.length) {
			profile.setQuestRelevance(values[payload.relevance()]);
		}
		profile.setTargetableByHostiles(payload.targetable());
		if (!pathId.equals(profile.getAssignedPathId())) {
			profile.setAssignedPathId(pathId);
			npc.setPatrolIndex(0);
			npc.setPatrolIdleTicks(-1);
		}
		profile.setAssignedDialogueId(dialogueId);
		profile.setLookAtPlayer(payload.lookAtPlayer());
		profile.setStationary(payload.stationary());
		profile.setInvulnerable(payload.invulnerable());
		profile.setSeated(payload.seated());
		float speed = payload.patrolSpeed();
		profile.setPatrolSpeed(Float.isNaN(speed) ? 1.0F : speed);
		npc.setProfile(profile);
		tell(player, "npcdirector.gui.applied");
		DebugLog.action("admin aplicado por " + player.getName() + " npc=" + npc.getEntityId());
	}

	public static void assignPath(EntityPlayerMP player, AssignPathPayload payload) {
		QuestNpcEntity npc = validate(player, payload.npcEntityId(), "asignar camino");
		if (npc == null) {
			return;
		}
		String pathId = payload.pathId();
		if (!pathId.isEmpty() && !WaypointPathManager.ids().contains(pathId)) {
			DebugLog.denied("admin asignar camino desconocido de " + player.getName());
			tell(player, "npcdirector.gui.rejected");
			return;
		}
		List<QuestNpcEntity> targets = new java.util.ArrayList<>();
		if (payload.radius()) {
			AxisAlignedBB box = player.getEntityBoundingBox().grow(ASSIGN_RADIUS);
			for (QuestNpcEntity other : player.world.getEntitiesWithinAABB(QuestNpcEntity.class, box)) {
				if (player.getDistance(other) <= ASSIGN_RADIUS) {
					targets.add(other);
				}
			}
			if (targets.size() > MAX_ASSIGN_RADIUS_NPCS) {
				DebugLog.denied("admin asignar camino masivo supera el limite: " + targets.size());
				tell(player, "npcdirector.gui.assign_limit");
				return;
			}
		} else {
			targets.add(npc);
		}
		for (QuestNpcEntity target : targets) {
			NpcProfile profile = target.getProfile();
			if (!pathId.equals(profile.getAssignedPathId())) {
				profile.setAssignedPathId(pathId);
				target.setPatrolIndex(0);
				target.setPatrolIdleTicks(-1);
			}
			target.setProfile(profile);
		}
		tell(player, payload.radius() ? "npcdirector.gui.assigned_radius" : "npcdirector.gui.assigned");
		DebugLog.action("admin camino asignado por " + player.getName() + " a " + targets.size() + " npc");
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

	public static void delete(EntityPlayerMP player, int entityId) {
		QuestNpcEntity npc = validate(player, entityId, "eliminar");
		if (npc == null) {
			return;
		}
		npc.setDead();
		tell(player, "npcdirector.gui.deleted");
		DebugLog.action("admin eliminado npc por " + player.getName());
	}

	public static void summon(EntityPlayerMP player) {
		if (!canEdit(player)) {
			DebugLog.denied("admin invocar denegado por permisos, modo jugador o no creativo: " + player.getName());
			return;
		}
		WorldServer level = player.getServerWorld();
		QuestNpcEntity npc = new QuestNpcEntity(level);
		npc.setLocationAndAngles(player.posX, player.posY, player.posZ, player.rotationYaw, 0.0F);
		npc.onInitialSpawn(level.getDifficultyForLocation(player.getPosition()), null);
		level.spawnEntity(npc);
		tell(player, "npcdirector.gui.summoned");
		DebugLog.action("admin invocado npc por " + player.getName());
		openFor(player, npc);
	}
}
