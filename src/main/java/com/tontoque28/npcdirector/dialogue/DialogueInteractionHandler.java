package com.tontoque28.npcdirector.dialogue;

import com.tontoque28.npcdirector.output.CommandRunner;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.quest.QuestManager;
import com.tontoque28.npcdirector.util.DebugLog;
import net.minecraft.world.entity.Entity;
import com.tontoque28.npcdirector.network.NetworkingInit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DialogueInteractionHandler {

	private record Session(String dialogueId, UUID npcId, String nodeId) {
	}

	private static final Map<UUID, Session> ACTIVE_SESSIONS = new HashMap<>();

	public static DialogueNode open(ServerPlayer player, QuestNpcEntity npc) {
		if (!npc.getProfile().isEnabled()) {
			return null;
		}
		String dialogueId = npc.getProfile().getAssignedDialogueId();
		if (dialogueId == null || dialogueId.isEmpty()) {
			return null;
		}
		Dialogue dialogue = DialogueManager.get(dialogueId);
		if (dialogue == null) {
			return null;
		}

		QuestManager quests = QuestManager.get((ServerLevel) player.level());
		QuestStateProvider provider = quests.providerFor(player.getUUID());
		DialogueNode entryNode = dialogue.resolveEntryNode(provider);
		if (entryNode == null) {
			return null;
		}

		ACTIVE_SESSIONS.put(player.getUUID(), new Session(dialogueId, npc.getUUID(), entryNode.getId()));
		DebugLog.action("dialogue opened: " + dialogueId + " node=" + entryNode.getId() + " player=" + player.getName().getString());
		return entryNode;
	}

	public static DialogueNode choose(ServerPlayer player, int optionIndex) {
		Session session = ACTIVE_SESSIONS.get(player.getUUID());
		if (session == null) {
			return null;
		}
		if (!npcReachable(player, session)) {
			ACTIVE_SESSIONS.remove(player.getUUID());
			return null;
		}
		Dialogue dialogue = DialogueManager.get(session.dialogueId());
		if (dialogue == null) {
			ACTIVE_SESSIONS.remove(player.getUUID());
			return null;
		}
		DialogueNode currentNode = dialogue.getNode(session.nodeId());
		if (currentNode == null || optionIndex < 0 || optionIndex >= currentNode.getOptions().size()) {
			ACTIVE_SESSIONS.remove(player.getUUID());
			return null;
		}

		DialogueOption option = currentNode.getOptions().get(optionIndex);
		applyEffects(player, option);

		String nextNodeId = option.getNextNodeId();
		if (nextNodeId == null || nextNodeId.isEmpty()) {
			ACTIVE_SESSIONS.remove(player.getUUID());
			return null;
		}

		DialogueNode nextNode = dialogue.getNode(nextNodeId);
		if (nextNode == null) {
			ACTIVE_SESSIONS.remove(player.getUUID());
			return null;
		}

		ACTIVE_SESSIONS.put(player.getUUID(), new Session(session.dialogueId(), session.npcId(), nextNode.getId()));
		return nextNode;
	}

	public static String npcName(ServerPlayer player) {
		Session session = ACTIVE_SESSIONS.get(player.getUUID());
		if (session == null || !(player.level() instanceof ServerLevel level)) {
			return "";
		}
		Entity npc = level.getEntity(session.npcId());
		if (npc instanceof QuestNpcEntity quest) {
			String name = quest.getProfile().getDisplayName();
			return name == null ? "" : (name.length() > 64 ? name.substring(0, 64) : name);
		}
		return "";
	}

	public static final double MAX_DISTANCE = 10.0;

	private static boolean npcReachable(ServerPlayer player, Session session) {
		if (!(player.level() instanceof ServerLevel level)) {
			return false;
		}
		Entity npc = level.getEntity(session.npcId());
		return npc instanceof QuestNpcEntity && npc.isAlive() && player.distanceTo(npc) <= MAX_DISTANCE;
	}

	public static void clearAll() {
		ACTIVE_SESSIONS.clear();
	}

	public static boolean hasSession(UUID playerId) {
		return ACTIVE_SESSIONS.containsKey(playerId);
	}

	public static void tick(net.minecraft.server.MinecraftServer server) {
		if (ACTIVE_SESSIONS.isEmpty()) {
			return;
		}
		for (Map.Entry<UUID, Session> entry : new java.util.ArrayList<>(ACTIVE_SESSIONS.entrySet())) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				ACTIVE_SESSIONS.remove(entry.getKey());
				continue;
			}
			if (!npcReachable(player, entry.getValue())) {
				ACTIVE_SESSIONS.remove(entry.getKey());
				NetworkingInit.sendNodeOrClose(player, null);
				DebugLog.action("dialogue closed by distance/death: " + player.getName().getString());
			}
		}
	}

	public static void close(ServerPlayer player) {
		ACTIVE_SESSIONS.remove(player.getUUID());
	}

	private static void applyEffects(ServerPlayer player, DialogueOption option) {
		if (option.getSetQuestId() != null && option.getSetQuestState() != null) {
			QuestManager quests = QuestManager.get((ServerLevel) player.level());
			quests.setPlayerState(player.getUUID(), option.getSetQuestId(), option.getSetQuestState());
			DebugLog.action("dialogue setQuest: " + option.getSetQuestId() + "=" + option.getSetQuestState());
		}

		if (option.getCommandsToRun() != null && !option.getCommandsToRun().isEmpty()) {
			for (String command : option.getCommandsToRun()) {
				CommandRunner.run(player, command, "dialogo");
			}
		}
	}
}
