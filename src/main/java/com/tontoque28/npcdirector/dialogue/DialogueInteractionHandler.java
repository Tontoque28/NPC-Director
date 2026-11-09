package com.tontoque28.npcdirector.dialogue;

import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.quest.QuestManager;
import com.tontoque28.npcdirector.util.DebugLog;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

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

	private static final class PlayerSource implements ICommandSender {
		private final EntityPlayerMP player;

		private PlayerSource(EntityPlayerMP player) {
			this.player = player;
		}

		@Override
		public String getName() {
			return player.getName();
		}

		@Override
		public ITextComponent getDisplayName() {
			return player.getDisplayName();
		}

		@Override
		public void sendMessage(ITextComponent component) {
			TextComponentString prefix = new TextComponentString("[NPCDirector] El comando fallo: ");
			prefix.appendSibling(component);
			player.sendMessage(prefix);
		}

		@Override
		public boolean canUseCommand(int permLevel, String commandName) {
			return true;
		}

		@Override
		public World getEntityWorld() {
			return player.world;
		}

		@Override
		public Entity getCommandSenderEntity() {
			return player;
		}

		@Override
		public BlockPos getPosition() {
			return player.getPosition();
		}

		@Override
		public Vec3d getPositionVector() {
			return player.getPositionVector();
		}

		@Override
		public boolean sendCommandFeedback() {
			return false;
		}

		@Override
		public MinecraftServer getServer() {
			return player.getServer();
		}
	}

	private static final Map<UUID, Session> ACTIVE_SESSIONS = new HashMap<>();

	public static DialogueNode open(EntityPlayerMP player, QuestNpcEntity npc) {
		String dialogueId = npc.getProfile().getAssignedDialogueId();
		if (dialogueId == null || dialogueId.isEmpty()) {
			return null;
		}
		Dialogue dialogue = DialogueManager.get(dialogueId);
		if (dialogue == null) {
			return null;
		}

		QuestManager quests = QuestManager.get((WorldServer) player.world);
		QuestStateProvider provider = quests.providerFor(player.getUniqueID());
		DialogueNode entryNode = dialogue.resolveEntryNode(provider);
		if (entryNode == null) {
			return null;
		}

		ACTIVE_SESSIONS.put(player.getUniqueID(), new Session(dialogueId, npc.getUniqueID(), entryNode.getId()));
		DebugLog.action("dialogue opened: " + dialogueId + " node=" + entryNode.getId() + " player=" + player.getName());
		return entryNode;
	}

	public static DialogueNode choose(EntityPlayerMP player, int optionIndex) {
		Session session = ACTIVE_SESSIONS.get(player.getUniqueID());
		if (session == null) {
			return null;
		}
		if (!npcReachable(player, session)) {
			ACTIVE_SESSIONS.remove(player.getUniqueID());
			return null;
		}
		Dialogue dialogue = DialogueManager.get(session.dialogueId());
		if (dialogue == null) {
			ACTIVE_SESSIONS.remove(player.getUniqueID());
			return null;
		}
		DialogueNode currentNode = dialogue.getNode(session.nodeId());
		if (currentNode == null || optionIndex < 0 || optionIndex >= currentNode.getOptions().size()) {
			ACTIVE_SESSIONS.remove(player.getUniqueID());
			return null;
		}

		DialogueOption option = currentNode.getOptions().get(optionIndex);
		applyEffects(player, option);

		String nextNodeId = option.getNextNodeId();
		if (nextNodeId == null || nextNodeId.isEmpty()) {
			ACTIVE_SESSIONS.remove(player.getUniqueID());
			return null;
		}

		DialogueNode nextNode = dialogue.getNode(nextNodeId);
		if (nextNode == null) {
			ACTIVE_SESSIONS.remove(player.getUniqueID());
			return null;
		}

		ACTIVE_SESSIONS.put(player.getUniqueID(), new Session(session.dialogueId(), session.npcId(), nextNode.getId()));
		return nextNode;
	}

	private static boolean npcReachable(EntityPlayerMP player, Session session) {
		Entity npc = player.getServerWorld().getEntityFromUuid(session.npcId());
		return npc instanceof QuestNpcEntity && npc.isEntityAlive() && player.getDistance(npc) <= MAX_DISTANCE;
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
			EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(entry.getKey());
			if (player == null) {
				ACTIVE_SESSIONS.remove(entry.getKey());
				continue;
			}
			if (!npcReachable(player, entry.getValue())) {
				ACTIVE_SESSIONS.remove(entry.getKey());
				NetworkingInit.sendNodeOrClose(player, null);
				DebugLog.action("dialogue closed by distance/death: " + player.getName());
			}
		}
	}

	public static void close(EntityPlayerMP player) {
		ACTIVE_SESSIONS.remove(player.getUniqueID());
	}

	private static void applyEffects(EntityPlayerMP player, DialogueOption option) {
		if (option.getSetQuestId() != null && option.getSetQuestState() != null) {
			QuestManager quests = QuestManager.get((WorldServer) player.world);
			quests.setPlayerState(player.getUniqueID(), option.getSetQuestId(), option.getSetQuestState());
			DebugLog.action("dialogue setQuest: " + option.getSetQuestId() + "=" + option.getSetQuestState());
		}

		if (option.getCommandsToRun() != null && !option.getCommandsToRun().isEmpty()) {
			MinecraftServer server = player.getServer();
			ICommandSender source = new PlayerSource(player);
			for (String command : option.getCommandsToRun()) {
				try {
					server.getCommandManager().executeCommand(source, command);
					DebugLog.action("dialogue command executed: " + command);
				} catch (Exception exception) {
					DebugLog.error("dialogue command failed: " + command + " - " + exception.getMessage());
					NpcDirector.LOGGER.error("Dialogue command failed: {}", command, exception);
					player.sendMessage(new TextComponentString("[NPCDirector] El comando fallo: " + command));
				}
			}
		}
	}
}
