package com.tontoque28.npcdirector.dialogue;

import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.quest.QuestManager;
import com.tontoque28.npcdirector.util.DebugLog;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DialogueInteractionHandler {

	public static final double MAX_DISTANCE = 10.0;

	private static final class Session {
		private final String dialogueId;
		private final UUID npcId;
		private final String nodeId;

		private Session(String dialogueId, UUID npcId, String nodeId) {
			this.dialogueId = dialogueId;
			this.npcId = npcId;
			this.nodeId = nodeId;
		}

		private String dialogueId() {
			return dialogueId;
		}

		private UUID npcId() {
			return npcId;
		}

		private String nodeId() {
			return nodeId;
		}
	}

	private static final Map<UUID, Session> ACTIVE_SESSIONS = new HashMap<>();

	public static DialogueNode open(ServerPlayerEntity player, QuestNpcEntity npc) {
		String dialogueId = npc.getProfile().getAssignedDialogueId();
		if (dialogueId == null || dialogueId.isEmpty()) {
			return null;
		}
		Dialogue dialogue = DialogueManager.get(dialogueId);
		if (dialogue == null) {
			return null;
		}

		QuestManager quests = QuestManager.get((ServerWorld) player.level);
		QuestStateProvider provider = quests.providerFor(player.getUUID());
		DialogueNode entryNode = dialogue.resolveEntryNode(provider);
		if (entryNode == null) {
			return null;
		}

		ACTIVE_SESSIONS.put(player.getUUID(), new Session(dialogueId, npc.getUUID(), entryNode.getId()));
		DebugLog.action("dialogue opened: " + dialogueId + " node=" + entryNode.getId() + " player=" + player.getName().getString());
		return entryNode;
	}

	public static DialogueNode choose(ServerPlayerEntity player, int optionIndex) {
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

	private static boolean npcReachable(ServerPlayerEntity player, Session session) {
		if (!(player.level instanceof ServerWorld)) {
			return false;
		}
		Entity npc = ((ServerWorld) player.level).getEntity(session.npcId());
		return npc instanceof QuestNpcEntity && npc.isAlive() && player.distanceTo(npc) <= MAX_DISTANCE;
	}

	public static void clearAll() {
		ACTIVE_SESSIONS.clear();
	}

	public static boolean hasSession(UUID playerId) {
		return ACTIVE_SESSIONS.containsKey(playerId);
	}

	public static void tick(MinecraftServer server) {
		if (server == null || ACTIVE_SESSIONS.isEmpty()) {
			return;
		}
		for (Map.Entry<UUID, Session> entry : new ArrayList<>(ACTIVE_SESSIONS.entrySet())) {
			ServerPlayerEntity player = server.getPlayerList().getPlayer(entry.getKey());
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

	public static void close(ServerPlayerEntity player) {
		ACTIVE_SESSIONS.remove(player.getUUID());
	}

	private static void applyEffects(ServerPlayerEntity player, DialogueOption option) {
		if (option.getSetQuestId() != null && option.getSetQuestState() != null) {
			QuestManager quests = QuestManager.get((ServerWorld) player.level);
			quests.setPlayerState(player.getUUID(), option.getSetQuestId(), option.getSetQuestState());
			DebugLog.action("dialogue setQuest: " + option.getSetQuestId() + "=" + option.getSetQuestState());
		}

		if (option.getCommandsToRun() != null && !option.getCommandsToRun().isEmpty()) {
			ServerWorld level = (ServerWorld) player.level;
			ICommandSource sink = new ICommandSource() {
				@Override
				public void sendMessage(ITextComponent message, UUID sender) {
					player.sendMessage(new StringTextComponent("[NPCDirector] El comando fallo: ").append(message), Util.NIL_UUID);
				}

				@Override
				public boolean acceptsSuccess() {
					return false;
				}

				@Override
				public boolean acceptsFailure() {
					return true;
				}

				@Override
				public boolean shouldInformAdmins() {
					return false;
				}
			};
			CommandSource source = new CommandSource(sink, player.position(), player.getRotationVector(), level, 4, player.getName().getString(), player.getDisplayName(), level.getServer(), player);
			for (String command : option.getCommandsToRun()) {
				try {
					level.getServer().getCommands().performCommand(source, command);
					DebugLog.action("dialogue command executed: " + command);
				} catch (Exception exception) {
					DebugLog.error("dialogue command failed: " + command + " - " + exception.getMessage());
					NpcDirector.LOGGER.error("Dialogue command failed: {}", command, exception);
					player.sendMessage(new StringTextComponent("[NPCDirector] El comando fallo: " + command), Util.NIL_UUID);
				}
			}
		}
	}
}
