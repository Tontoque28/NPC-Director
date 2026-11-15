package com.tontoque28.npcdirector.admin;

import com.tontoque28.npcdirector.util.Texts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.config.ModConfig;
import com.tontoque28.npcdirector.dialogue.Dialogue;
import com.tontoque28.npcdirector.dialogue.DialogueNode;
import com.tontoque28.npcdirector.dialogue.DialogueOption;
import com.tontoque28.npcdirector.output.OutputService;
import com.tontoque28.npcdirector.waypoint.Waypoint;
import com.tontoque28.npcdirector.waypoint.WaypointPath;
import com.tontoque28.npcdirector.world.WorldContent;
import com.tontoque28.npcdirector.network.DialogueData;
import com.tontoque28.npcdirector.network.PathData;
import com.tontoque28.npcdirector.network.SaveDialoguePayload;
import com.tontoque28.npcdirector.network.SavePathPayload;
import com.tontoque28.npcdirector.data.NpcProfile;
import com.tontoque28.npcdirector.data.QuestRelevance;
import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.entity.ModEntities;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.network.AdminFields;
import com.tontoque28.npcdirector.network.ApplyAdminPayload;
import com.tontoque28.npcdirector.network.ApplySkinPayload;
import com.tontoque28.npcdirector.network.AssignPathPayload;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.util.DebugLog;
import com.tontoque28.npcdirector.util.Validate;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

public class AdminService {

	public static final double MAX_DISTANCE = 8.0;
	public static final double LOOK_DISTANCE = 6.0;
	public static final int MAX_EDITOR_NODES = 16;
	public static final int PATH_WAIT_TICKS = 40;
	public static final int MAX_EDITOR_COMMANDS = 16;
	public static final int MAX_EDITOR_COMMAND = 256;
	public static final long SKIN_COOLDOWN_TICKS = 10L;

	private static final java.util.Map<UUID, Long> SKIN_LAST = new java.util.HashMap<>();

	public static void say(ServerPlayerEntity player, ITextComponent message) {
		Texts.tell(player, message);
		NetworkingInit.sendPanel(player, message);
	}

	public static boolean isOperator(ServerPlayerEntity player) {
		return player.hasPermissions(2);
	}

	public static boolean canEdit(ServerPlayerEntity player) {
		return isOperator(player) && ModConfig.isDevMode() && player.isCreative();
	}

	public static QuestNpcEntity validate(ServerPlayerEntity player, int entityId, String action) {
		if (!canEdit(player)) {
			DebugLog.denied("admin " + action + " denegado por permisos, modo jugador o modo no creativo: " + player.getName().getString());
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

	public static AdminFields fieldsOf(QuestNpcEntity npc) {
		NpcProfile profile = npc.getProfile();
		List<String> equip = new ArrayList<>();
		for (EquipmentSlotType slot : QuestNpcEntity.EQUIP_SLOTS) {
			equip.add(cut(npc.equipId(slot), AdminFields.EQUIP_MAX));
		}
		return new AdminFields(profile.getScale(), profile.getArmModel(), profile.isNameVisible(), profile.getNameColor(), equip,
				profile.isLookAtPlayer(), profile.isStationary(), profile.isInvulnerable(), profile.getPatrolSpeed(), profile.isSitting(),
				cut(profile.getGreeting(), AdminFields.GREETING_MAX), profile.getGreetRange(), cut(profile.getSoundId(), AdminFields.SOUND_MAX),
				profile.getSoundVolume(), profile.getSoundPitch(), profile.isEnabled());
	}

	private static String cut(String text, int max) {
		if (text == null) {
			return "";
		}
		return text.length() > max ? text.substring(0, max) : text;
	}

	private static List<String> shortIds(Collection<String> ids) {
		return ids.stream().filter(id -> id.length() <= 128).limit(256).collect(Collectors.toList());
	}

	public static void openFor(ServerPlayerEntity player, QuestNpcEntity npc) {
		if (!canEdit(player)) {
			DebugLog.denied("panel de edicion denegado por permisos, modo jugador o modo no creativo: " + player.getName().getString());
			return;
		}
		npc.holdForEdit();
		NpcProfile profile = npc.getProfile();
		NetworkingInit.sendToPlayer(player, new OpenAdminPayload(npc.getId(), cut(profile.getDisplayName(), 64),
				profile.getQuestRelevance().ordinal(), profile.isTargetableByHostiles(),
				cut(profile.getAssignedPathId(), 128), cut(profile.getAssignedDialogueId(), 128),
				shortIds(WaypointPathManager.ids()), shortIds(DialogueManager.ids()), cut(profile.getSkinName(), ApplySkinPayload.NAME_MAX), fieldsOf(npc),
				ModConfig.isDevMode(), pathDataOf(profile.getAssignedPathId()), dialogueDataOf(profile.getAssignedDialogueId())));
	}

	public static void openSummonPanel(ServerPlayerEntity player) {
		if (!canEdit(player)) {
			DebugLog.denied("panel de invocacion denegado por permisos, modo jugador o modo no creativo: " + player.getName().getString());
			return;
		}
		NetworkingInit.sendToPlayer(player, new OpenAdminPayload(-1, "", 0, false, "", "",
				shortIds(WaypointPathManager.ids()), shortIds(DialogueManager.ids()), "", AdminFields.defaults(),
				ModConfig.isDevMode(), PathData.empty(), DialogueData.empty()));
	}

	public static PathData pathDataOf(String id) {
		WaypointPath path = id == null || id.isEmpty() ? null : WaypointPathManager.get(id);
		if (path == null) {
			return PathData.empty();
		}
		List<int[]> points = new ArrayList<>();
		for (Waypoint point : path.getPoints()) {
			if (points.size() >= PathData.MAX_POINTS) {
				break;
			}
			points.add(new int[] {point.getPosition().getX(), point.getPosition().getY(), point.getPosition().getZ()});
		}
		return new PathData(path.isLoop(), points);
	}

	public static DialogueData dialogueDataOf(String id) {
		if (id == null || !id.startsWith(WorldContent.NAMESPACE + ":")) {
			return DialogueData.empty();
		}
		Dialogue dialogue = DialogueManager.get(id);
		if (dialogue == null) {
			return DialogueData.empty();
		}
		List<DialogueData.Node> nodes = new ArrayList<>();
		for (DialogueNode node : dialogue.getNodes()) {
			if (nodes.size() >= MAX_EDITOR_NODES) {
				break;
			}
			List<DialogueData.Opt> options = new ArrayList<>();
			for (DialogueOption option : node.getOptions()) {
				if (options.size() >= DialogueManager.MAX_OPTIONS) {
					break;
				}
				List<String> commands = option.getCommands() == null ? new ArrayList<>() : new ArrayList<>(option.getCommands());
				options.add(new DialogueData.Opt(option.getResponseText(), commands, option.getNextNodeId() == null ? "" : option.getNextNodeId(),
						option.getSetQuestId() == null ? "" : option.getSetQuestId(), option.getSetQuestState() == null ? "" : option.getSetQuestState()));
			}
			nodes.add(new DialogueData.Node(node.getId(), node.getText(), options));
		}
		DialogueData data = new DialogueData(nodes);
		return data.estimateBytes() > DialogueData.SAFE_BYTES ? DialogueData.empty() : data;
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
		npc.releaseEditHold();
		NpcProfile profile = npc.getProfile();
		String pathId = payload.pathId();
		String dialogueId = payload.dialogueId();
		boolean pathOk = pathId.isEmpty() || pathId.equals(profile.getAssignedPathId()) || WaypointPathManager.ids().contains(pathId);
		boolean dialogueOk = dialogueId.isEmpty() || dialogueId.equals(profile.getAssignedDialogueId()) || DialogueManager.ids().contains(dialogueId);
		if (!pathOk || !dialogueOk) {
			reject(player, "camino o dialogo desconocido");
			return;
		}
		AdminFields fields = payload.fields();
		if (!Validate.finite(fields.scale()) || !Validate.finite(fields.patrolSpeed()) || !Validate.finite(fields.soundVolume()) || !Validate.finite(fields.soundPitch())) {
			reject(player, "numero no valido");
			return;
		}
		if (fields.equip().size() != AdminFields.EQUIP_COUNT) {
			reject(player, "equipo con tamano incorrecto");
			return;
		}
		for (int i = 0; i < AdminFields.EQUIP_COUNT; i++) {
			String id = fields.equip().get(i);
			if (!Validate.isClear(id) && Validate.item(id) == null) {
				reject(player, "objeto inexistente en " + QuestNpcEntity.EQUIP_NAMES[i] + ": " + Validate.cleanText(id, 40));
				return;
			}
		}
		String soundId = fields.soundId() == null ? "" : fields.soundId().trim().toLowerCase();
		if (!Validate.isClear(soundId) && Validate.sound(soundId) == null) {
			reject(player, "sonido inexistente: " + Validate.cleanText(soundId, 40));
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
		profile.setScale(Validate.clampFloat(fields.scale(), Validate.SCALE_MIN, Validate.SCALE_MAX, 1.0F));
		profile.setArmModel(Math.max(0, Math.min(2, fields.armModel())));
		profile.setNameVisible(fields.nameVisible());
		profile.setNameColor(Math.max(0, Math.min(15, fields.nameColor())));
		profile.setLookAtPlayer(fields.lookAtPlayer());
		profile.setStationary(fields.stationary());
		profile.setInvulnerable(fields.invulnerable());
		profile.setPatrolSpeed(Validate.clampFloat(fields.patrolSpeed(), Validate.SPEED_MIN, Validate.SPEED_MAX, 1.0F));
		profile.setSitting(fields.sitting());
		profile.setGreeting(Validate.cleanText(fields.greeting(), Validate.GREETING_MAX));
		profile.setGreetRange(Math.max(1, Math.min(16, fields.greetRange())));
		profile.setSoundId(Validate.isClear(soundId) ? "" : soundId);
		profile.setSoundVolume(Validate.clampFloat(fields.soundVolume(), 0.0F, 2.0F, 1.0F));
		profile.setSoundPitch(Validate.clampFloat(fields.soundPitch(), 0.5F, 2.0F, 1.0F));
		boolean wasEnabled = profile.isEnabled();
		profile.setEnabled(fields.enabled());
		for (int i = 0; i < AdminFields.EQUIP_COUNT; i++) {
			setEquip(npc, i, fields.equip().get(i));
		}
		npc.setProfile(profile);
		say(player, Texts.translatable("npcdirector.gui.applied"));
		DebugLog.action("admin aplicado por " + player.getName().getString() + " npc=" + npc.getId());
		OutputService.line("[NPC " + npc.getId() + "] panel aplicado por " + player.getName().getString() + (wasEnabled != fields.enabled() ? " estado=" + (fields.enabled() ? "ON" : "OFF") : ""));
	}

	public static final double BULK_RADIUS = 16.0;
	public static final int BULK_MAX_NPCS = 32;

	public static void assignPath(ServerPlayerEntity player, AssignPathPayload payload) {
		QuestNpcEntity npc = validate(player, payload.npcEntityId(), "asignar camino");
		if (npc == null) {
			return;
		}
		String pathId = payload.pathId() == null ? "" : payload.pathId().trim();
		if (!pathId.isEmpty() && !WaypointPathManager.ids().contains(pathId)) {
			DebugLog.denied("asignar camino desconocido de " + player.getName().getString() + ": " + Validate.cleanText(pathId, 40));
			say(player, Texts.translatable("npcdirector.gui.path_unknown"));
			return;
		}
		List<QuestNpcEntity> targets = new ArrayList<>();
		if (payload.radius()) {
			AxisAlignedBB box = player.getBoundingBox().inflate(BULK_RADIUS);
			for (QuestNpcEntity other : player.level.getEntitiesOfClass(QuestNpcEntity.class, box)) {
				if (other.distanceTo(player) <= BULK_RADIUS) {
					targets.add(other);
				}
			}
			if (!targets.contains(npc)) {
				targets.add(npc);
			}
			if (targets.size() > BULK_MAX_NPCS) {
				DebugLog.denied("asignar camino a " + targets.size() + " npc rechazado de " + player.getName().getString() + " (limite " + BULK_MAX_NPCS + ")");
				OutputService.line("[NPC] asignar camino rechazado: " + targets.size() + " NPC en radio (limite " + BULK_MAX_NPCS + ")");
				say(player, Texts.translatable("npcdirector.gui.path_too_many", targets.size(), (int) BULK_RADIUS, BULK_MAX_NPCS));
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
		String shown = pathId.isEmpty() ? "-" : pathId;
		if (payload.radius()) {
			say(player, Texts.translatable("npcdirector.gui.path_assigned_all", shown, targets.size(), (int) BULK_RADIUS));
		} else {
			say(player, Texts.translatable("npcdirector.gui.path_assigned", shown));
		}
		OutputService.line("[NPC " + npc.getId() + "] camino " + shown + " asignado a " + targets.size() + " NPC por " + player.getName().getString());
		DebugLog.action("camino " + shown + " asignado a " + targets.size() + " npc por " + player.getName().getString());
	}

	public static void savePath(ServerPlayerEntity player, SavePathPayload payload) {
		QuestNpcEntity npc = validate(player, payload.npcEntityId(), "guardar camino");
		if (npc == null) {
			return;
		}
		String name = worldName(payload.name());
		PathData data = payload.path();
		if (!WorldContent.validName(name)) {
			rejectEditor(player, "camino", "nombre no valido: " + Validate.cleanText(payload.name(), 40));
			return;
		}
		if (data.points().isEmpty() || data.points().size() > PathData.MAX_POINTS) {
			rejectEditor(player, "camino", "numero de puntos no valido: " + data.points().size());
			return;
		}
		JsonArray points = new JsonArray();
		for (int[] point : data.points()) {
			if (Math.abs(point[0]) > 30000000 || Math.abs(point[2]) > 30000000 || point[1] < -2048 || point[1] > 4096) {
				rejectEditor(player, "camino", "coordenada fuera de rango");
				return;
			}
			JsonObject entry = new JsonObject();
			entry.addProperty("x", point[0]);
			entry.addProperty("y", point[1]);
			entry.addProperty("z", point[2]);
			entry.addProperty("wait", PATH_WAIT_TICKS);
			points.add(entry);
		}
		JsonObject root = new JsonObject();
		root.addProperty("loop", data.loop());
		root.add("points", points);
		WorldContent content = WorldContent.get(player.getServer());
		if (!content.putPath(name, root.toString())) {
			rejectEditor(player, "camino", "demasiados caminos guardados");
			return;
		}
		NpcProfile profile = npc.getProfile();
		String id = WorldContent.NAMESPACE + ":" + name;
		if (!id.equals(profile.getAssignedPathId())) {
			profile.setAssignedPathId(id);
			npc.setPatrolIndex(0);
			npc.setPatrolIdleTicks(-1);
		}
		npc.setProfile(profile);
		say(player, Texts.translatable("npcdirector.gui.path_saved", id));
		OutputService.line("[NPC " + npc.getId() + "] camino guardado " + id + " puntos=" + data.points().size() + " loop=" + data.loop());
		DebugLog.action("camino de mundo guardado " + id + " por " + player.getName().getString());
	}

	public static void saveDialogue(ServerPlayerEntity player, SaveDialoguePayload payload) {
		QuestNpcEntity npc = validate(player, payload.npcEntityId(), "guardar dialogo");
		if (npc == null) {
			return;
		}
		String name = worldName(payload.name());
		if (!WorldContent.validName(name)) {
			rejectEditor(player, "dialogo", "nombre no valido: " + Validate.cleanText(payload.name(), 40));
			return;
		}
		List<DialogueData.Node> nodes = payload.dialogue().nodes();
		if (nodes.isEmpty() || nodes.size() > MAX_EDITOR_NODES) {
			rejectEditor(player, "dialogo", "numero de nodos no valido: " + nodes.size());
			return;
		}
		Set<String> ids = new HashSet<>();
		for (DialogueData.Node node : nodes) {
			if (!Validate.nodeId(node.id()) || !ids.add(node.id())) {
				rejectEditor(player, "dialogo", "id de nodo no valido o repetido: " + Validate.cleanText(node.id(), 40));
				return;
			}
		}
		boolean canCommand = player.hasPermissions(4);
		JsonArray nodeArray = new JsonArray();
		for (DialogueData.Node node : nodes) {
			JsonObject nodeObject = new JsonObject();
			nodeObject.addProperty("id", node.id());
			nodeObject.addProperty("text", Validate.cleanMultiline(node.text(), DialogueManager.MAX_TEXT));
			JsonArray optionArray = new JsonArray();
			int count = 0;
			for (DialogueData.Opt option : node.options()) {
				if (count++ >= DialogueManager.MAX_OPTIONS) {
					break;
				}
				JsonObject optionObject = new JsonObject();
				String text = Validate.cleanText(option.text(), DialogueManager.MAX_OPTION_TEXT);
				if (text.isEmpty()) {
					rejectEditor(player, "dialogo", "boton sin texto en el nodo " + node.id());
					return;
				}
				optionObject.addProperty("text", text);
				String next = option.next() == null ? "" : option.next().trim();
				if (!next.isEmpty() && !ids.contains(next)) {
					rejectEditor(player, "dialogo", "siguiente nodo inexistente: " + Validate.cleanText(next, 40));
					return;
				}
				if (!next.isEmpty()) {
					optionObject.addProperty("next", next);
				}
				JsonArray commandArray = new JsonArray();
				for (String raw : option.commands()) {
					String command = Validate.cleanText(raw, MAX_EDITOR_COMMAND);
					if (command.startsWith("/")) {
						command = command.substring(1).trim();
					}
					if (command.isEmpty()) {
						continue;
					}
					if (commandArray.size() >= MAX_EDITOR_COMMANDS) {
						break;
					}
					commandArray.add(command);
				}
				if (commandArray.size() > 0) {
					if (!canCommand) {
						rejectEditor(player, "dialogo", "comandos requieren permiso de nivel 4");
						say(player, Texts.translatable("npcdirector.gui.need_level4"));
						return;
					}
					optionObject.add("commands", commandArray);
				}
				String questId = option.questId() == null ? "" : option.questId().trim();
				String questState = option.questState() == null ? "" : option.questState().trim();
				if (!questId.isEmpty() || !questState.isEmpty()) {
					if (!Validate.questToken(questId) || !Validate.questToken(questState)) {
						rejectEditor(player, "dialogo", "mision o estado no validos");
						return;
					}
					JsonObject quest = new JsonObject();
					quest.addProperty("quest", questId);
					quest.addProperty("state", questState);
					optionObject.add("setQuest", quest);
				}
				optionArray.add(optionObject);
			}
			nodeObject.add("options", optionArray);
			nodeArray.add(nodeObject);
		}
		JsonObject root = new JsonObject();
		root.add("nodes", nodeArray);
		WorldContent content = WorldContent.get(player.getServer());
		if (!content.putDialogue(name, root.toString())) {
			rejectEditor(player, "dialogo", "demasiados dialogos guardados");
			return;
		}
		NpcProfile profile = npc.getProfile();
		profile.setAssignedDialogueId(WorldContent.NAMESPACE + ":" + name);
		npc.setProfile(profile);
		say(player, Texts.translatable("npcdirector.gui.dialogue_saved", WorldContent.NAMESPACE + ":" + name));
		OutputService.line("[NPC " + npc.getId() + "] dialogo guardado mundo:" + name + " nodos=" + nodes.size());
		DebugLog.action("dialogo de mundo guardado mundo:" + name + " por " + player.getName().getString());
	}

	private static String worldName(String raw) {
		String name = raw == null ? "" : raw.trim();
		String prefix = WorldContent.NAMESPACE + ":";
		return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
	}

	private static void rejectEditor(ServerPlayerEntity player, String what, String reason) {
		DebugLog.denied("guardar " + what + " rechazado de " + player.getName().getString() + ": " + reason);
		OutputService.line("[NPC] guardar " + what + " rechazado: " + reason);
		say(player, Texts.translatable("npcdirector.gui.editor_rejected", what));
	}

	private static void reject(ServerPlayerEntity player, String reason) {
		DebugLog.denied("admin aplicar rechazado de " + player.getName().getString() + ": " + reason);
		OutputService.line("[NPC] panel rechazado: " + reason);
		say(player, Texts.translatable("npcdirector.gui.rejected"));
	}

	public static boolean setEquip(QuestNpcEntity npc, int index, String rawId) {
		if (index < 0 || index >= QuestNpcEntity.EQUIP_SLOTS.length) {
			return false;
		}
		EquipmentSlotType slot = QuestNpcEntity.EQUIP_SLOTS[index];
		if (Validate.isClear(rawId)) {
			if (!npc.getItemBySlot(slot).isEmpty()) {
				npc.setEquip(slot, ItemStack.EMPTY);
			}
			return true;
		}
		Item item = Validate.item(rawId);
		if (item == null) {
			return false;
		}
		if (npc.getItemBySlot(slot).getItem() != item) {
			npc.setEquip(slot, new ItemStack(item));
		}
		return true;
	}

	public static Consumer<ITextComponent> notifier(ServerPlayerEntity player, MinecraftServer server) {
		if (player != null) {
			UUID id = player.getUUID();
			return message -> {
				ServerPlayerEntity online = server.getPlayerList().getPlayer(id);
				if (online != null) {
					say(online, message);
				}
				NpcDirector.LOGGER.info("[NPCDirector] {}", message.getString());
			};
		}
		return message -> {
			for (ServerPlayerEntity online : server.getPlayerList().getPlayers()) {
				if (isOperator(online)) {
					say(online, message);
				}
			}
			NpcDirector.LOGGER.info("[NPCDirector] {}", message.getString());
		};
	}

	public static boolean startSkin(QuestNpcEntity npc, String rawName, Consumer<ITextComponent> notify) {
		String name = rawName == null ? "" : rawName.trim();
		if (name.isEmpty() || name.equalsIgnoreCase("none")) {
			npc.clearSkin();
			notify.accept(Texts.translatable("npcdirector.skin.cleared"));
			return true;
		}
		if (!Validate.validSkinName(name)) {
			notify.accept(Texts.translatable("npcdirector.skin.invalid"));
			return false;
		}
		NpcProfile profile = npc.getProfile();
		profile.setSkinName(name);
		profile.setSkinTexture("");
		profile.setSkinUuid("");
		profile.setSkinPng("");
		npc.setProfile(profile);
		notify.accept(Texts.translatable("npcdirector.skin.resolving", name));
		npc.resolveSkin(notify);
		return true;
	}

	public static void applySkin(ServerPlayerEntity player, ApplySkinPayload payload) {
		QuestNpcEntity npc = validate(player, payload.npcEntityId(), "skin");
		if (npc == null) {
			return;
		}
		String name = payload.skinName() == null ? "" : payload.skinName().trim();
		if (!name.isEmpty() && !name.equalsIgnoreCase("none") && !Validate.validSkinName(name)) {
			DebugLog.denied("admin skin nombre no valido de " + player.getName().getString() + ": " + Validate.cleanText(name, 40));
			say(player, Texts.translatable("npcdirector.skin.invalid"));
			return;
		}
		long now = player.getLevel().getGameTime();
		Long last = SKIN_LAST.get(player.getUUID());
		if (last != null && now >= last && now - last < SKIN_COOLDOWN_TICKS) {
			DebugLog.denied("admin skin demasiado rapido de " + player.getName().getString());
			say(player, Texts.translatable("npcdirector.skin.too_fast"));
			return;
		}
		SKIN_LAST.put(player.getUUID(), now);
		if (SKIN_LAST.size() > 64) {
			SKIN_LAST.values().removeIf(time -> now - time >= SKIN_COOLDOWN_TICKS);
		}
		startSkin(npc, name, notifier(player, player.getServer()));
		DebugLog.action("admin skin por " + player.getName().getString() + " npc=" + npc.getId() + " nombre=" + name);
	}

	public static String sanitizeName(String raw) {
		return Validate.cleanText(raw, 64);
	}

	public static void delete(ServerPlayerEntity player, int entityId) {
		QuestNpcEntity npc = validate(player, entityId, "eliminar");
		if (npc == null) {
			return;
		}
		npc.remove();
		say(player, Texts.translatable("npcdirector.gui.deleted"));
		DebugLog.action("admin eliminado npc por " + player.getName().getString());
	}

	public static void summon(ServerPlayerEntity player) {
		if (!canEdit(player)) {
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
		say(player, Texts.translatable("npcdirector.gui.summoned"));
		DebugLog.action("admin invocado npc por " + player.getName().getString());
		openFor(player, npc);
	}
}
