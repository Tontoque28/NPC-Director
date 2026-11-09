package com.tontoque28.npcdirector.command;

import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.config.ModConfig;
import com.tontoque28.npcdirector.data.NpcProfile;
import com.tontoque28.npcdirector.data.QuestRelevance;
import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.quest.QuestManager;
import com.tontoque28.npcdirector.util.DebugLog;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DevCommands extends CommandBase {

	private static final String USAGE = "/npcdirector <mode|debug|npc|quest|reload|gui>";

	@Override
	public String getName() {
		return "npcdirector";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return USAGE;
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0) {
			throw new WrongUsageException(USAGE);
		}
		switch (args[0]) {
			case "mode":
				mode(server, sender, args);
				return;
			case "debug":
				debug(server, sender, args);
				return;
			case "npc":
				npc(server, sender, args);
				return;
			case "quest":
				quest(server, sender, args);
				return;
			case "reload":
				reload(server, sender, args);
				return;
			case "gui":
				gui(server, sender, args);
				return;
			default:
				throw new WrongUsageException(USAGE);
		}
	}

	private void gui(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length > 2) {
			throw new WrongUsageException("/npcdirector gui [target]");
		}
		Entity origin = sender.getCommandSenderEntity();
		if (!(origin instanceof EntityPlayerMP)) {
			throw new PlayerNotFoundException("commands.generic.player.unspecified");
		}
		EntityPlayerMP player = (EntityPlayerMP) origin;
		if (args.length == 2) {
			QuestNpcEntity npc = asNpc(sender, getEntity(server, sender, args[1]));
			if (npc != null) {
				AdminService.openFor(player, npc);
			}
			return;
		}
		QuestNpcEntity npc = AdminService.lookedAt(player);
		if (npc == null) {
			AdminService.openSummonPanel(player);
			failure(sender, "[NPCDirector] No hay ningun NPC a la vista a menos de 6 bloques");
			return;
		}
		AdminService.openFor(player, npc);
	}

	private void mode(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length != 2) {
			throw new WrongUsageException("/npcdirector mode <dev|player>");
		}
		if (args[1].equals("dev")) {
			setMode(server, sender, true);
		} else if (args[1].equals("player")) {
			setMode(server, sender, false);
		} else {
			throw new WrongUsageException("/npcdirector mode <dev|player>");
		}
	}

	private void debug(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length != 2) {
			throw new WrongUsageException("/npcdirector debug <on|off|log|clear>");
		}
		switch (args[1]) {
			case "on":
				setDebug(server, sender, true);
				return;
			case "off":
				setDebug(server, sender, false);
				return;
			case "log":
				DebugLog.recent(20).forEach(line -> sender.sendMessage(new TextComponentString(line)));
				return;
			case "clear":
				DebugLog.clear();
				success(server, sender, "[NPCDirector] Log limpiado", true);
				return;
			default:
				throw new WrongUsageException("/npcdirector debug <on|off|log|clear>");
		}
	}

	private void npc(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 4) {
			throw new WrongUsageException("/npcdirector npc <target> <name|relevance|targetable|path|dialogue> <value>");
		}
		Entity target = getEntity(server, sender, args[1]);
		String property = args[2];
		String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
		switch (property) {
			case "name":
				npcName(server, sender, target, value);
				return;
			case "relevance":
				if (args.length != 4) {
					throw new WrongUsageException("/npcdirector npc <target> relevance <none|indirect|main>");
				}
				switch (args[3]) {
					case "none":
						npcRelevance(server, sender, target, QuestRelevance.NONE);
						return;
					case "indirect":
						npcRelevance(server, sender, target, QuestRelevance.INDIRECT);
						return;
					case "main":
						npcRelevance(server, sender, target, QuestRelevance.MAIN);
						return;
					default:
						throw new WrongUsageException("/npcdirector npc <target> relevance <none|indirect|main>");
				}
			case "targetable":
				if (args.length != 4) {
					throw new WrongUsageException("/npcdirector npc <target> targetable <true|false>");
				}
				npcTargetable(server, sender, target, parseBoolean(args[3]));
				return;
			case "path":
				npcPath(server, sender, target, value);
				return;
			case "dialogue":
				npcDialogue(server, sender, target, value);
				return;
			default:
				throw new WrongUsageException("/npcdirector npc <target> <name|relevance|targetable|path|dialogue> <value>");
		}
	}

	private void quest(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 3) {
			throw new WrongUsageException("/npcdirector quest <set|get> <questId> ...");
		}
		QuestManager manager = QuestManager.get(server.getWorld(0));
		if (args[1].equals("set")) {
			if (args.length < 4 || args.length > 5) {
				throw new WrongUsageException("/npcdirector quest set <questId> <state> [player]");
			}
			String questId = args[2];
			String state = args[3];
			if (args.length == 4) {
				manager.setGlobalState(questId, state);
				success(server, sender, "[NPCDirector] Quest global " + questId + " = " + state, true);
				DebugLog.action("quest global set: " + questId + "=" + state);
			} else {
				EntityPlayerMP player = getPlayer(server, sender, args[4]);
				manager.setPlayerState(player.getUniqueID(), questId, state);
				success(server, sender, "[NPCDirector] Quest " + questId + " = " + state + " para " + player.getName(), true);
				DebugLog.action("quest player set: " + player.getName() + " " + questId + "=" + state);
			}
		} else if (args[1].equals("get")) {
			if (args.length > 4) {
				throw new WrongUsageException("/npcdirector quest get <questId> [player]");
			}
			String questId = args[2];
			if (args.length == 3) {
				success(server, sender, "[NPCDirector] " + questId + " (global) = " + manager.getGlobalState(questId), false);
			} else {
				EntityPlayerMP player = getPlayer(server, sender, args[3]);
				UUID id = player.getUniqueID();
				success(server, sender, "[NPCDirector] " + questId + " (" + player.getName() + ") = " + manager.getPlayerState(id, questId), false);
			}
		} else {
			throw new WrongUsageException("/npcdirector quest <set|get> <questId> ...");
		}
	}

	private void reload(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length != 1) {
			throw new WrongUsageException("/npcdirector reload");
		}
		NpcDirector.reloadData(server);
		success(server, sender, "[NPCDirector] Datos recargados", true);
		DebugLog.action("data reloaded");
	}

	private static QuestNpcEntity asNpc(ICommandSender sender, Entity entity) {
		if (!(entity instanceof QuestNpcEntity)) {
			failure(sender, "[NPCDirector] El objetivo no es un NPC de este mod");
			return null;
		}
		return (QuestNpcEntity) entity;
	}

	private static void npcName(MinecraftServer server, ICommandSender sender, Entity entity, String value) {
		QuestNpcEntity npc = asNpc(sender, entity);
		if (npc == null) return;
		NpcProfile profile = npc.getProfile();
		profile.setDisplayName(value);
		npc.setProfile(profile);
		success(server, sender, "[NPCDirector] Nombre: " + value, true);
		DebugLog.action("npc name set: " + value);
	}

	private static void npcRelevance(MinecraftServer server, ICommandSender sender, Entity entity, QuestRelevance relevance) {
		QuestNpcEntity npc = asNpc(sender, entity);
		if (npc == null) return;
		NpcProfile profile = npc.getProfile();
		profile.setQuestRelevance(relevance);
		npc.setProfile(profile);
		success(server, sender, "[NPCDirector] Relevancia: " + relevance, true);
		DebugLog.action("npc relevance set: " + relevance);
	}

	private static void npcTargetable(MinecraftServer server, ICommandSender sender, Entity entity, boolean value) {
		QuestNpcEntity npc = asNpc(sender, entity);
		if (npc == null) return;
		npc.getProfile().setTargetableByHostiles(value);
		success(server, sender, "[NPCDirector] Targetable por hostiles: " + value, true);
		DebugLog.action("npc targetable set: " + value);
	}

	private static void npcPath(MinecraftServer server, ICommandSender sender, Entity entity, String value) {
		QuestNpcEntity npc = asNpc(sender, entity);
		if (npc == null) return;
		npc.getProfile().setAssignedPathId(value);
		npc.setPatrolIndex(0);
		npc.setPatrolIdleTicks(-1);
		success(server, sender, "[NPCDirector] Camino asignado: " + value, true);
		if (WaypointPathManager.get(value) == null) {
			failure(sender, "[NPCDirector] Aviso: el camino " + value + " no esta cargado");
		}
		DebugLog.action("npc path set: " + value);
	}

	private static void npcDialogue(MinecraftServer server, ICommandSender sender, Entity entity, String value) {
		QuestNpcEntity npc = asNpc(sender, entity);
		if (npc == null) return;
		npc.getProfile().setAssignedDialogueId(value);
		success(server, sender, "[NPCDirector] Diálogo asignado: " + value, true);
		if (DialogueManager.get(value) == null) {
			failure(sender, "[NPCDirector] Aviso: el dialogo " + value + " no esta cargado");
		}
		DebugLog.action("npc dialogue set: " + value);
	}

	private static void setMode(MinecraftServer server, ICommandSender sender, boolean dev) {
		ModConfig.setDevMode(dev);
		success(server, sender, "[NPCDirector] Modo: " + (dev ? "developer" : "player"), true);
		DebugLog.action("mode set to " + (dev ? "dev" : "player"));
	}

	private static void setDebug(MinecraftServer server, ICommandSender sender, boolean enabled) {
		DebugLog.setEnabled(enabled);
		success(server, sender, "[NPCDirector] Debug log: " + enabled, true);
	}

	private static void success(MinecraftServer server, ICommandSender sender, String message, boolean broadcast) {
		sender.sendMessage(new TextComponentString(message));
		if (!broadcast || !server.getWorld(0).getGameRules().getBoolean("sendCommandFeedback")) {
			return;
		}
		TextComponentString relay = new TextComponentString("[" + sender.getName() + ": " + message + "]");
		relay.getStyle().setColor(TextFormatting.GRAY).setItalic(true);
		for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
			if (player != sender && server.getPlayerList().canSendCommands(player.getGameProfile())) {
				player.sendMessage(relay);
			}
		}
		if (sender != server && server.getWorld(0).getGameRules().getBoolean("logAdminCommands")) {
			server.sendMessage(relay);
		}
	}

	private static void failure(ICommandSender sender, String message) {
		TextComponentString text = new TextComponentString(message);
		text.getStyle().setColor(TextFormatting.RED);
		sender.sendMessage(text);
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable net.minecraft.util.math.BlockPos targetPos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, "mode", "debug", "npc", "quest", "reload", "gui");
		}
		if (args.length == 2) {
			switch (args[0]) {
				case "mode":
					return getListOfStringsMatchingLastWord(args, "dev", "player");
				case "debug":
					return getListOfStringsMatchingLastWord(args, "on", "off", "log", "clear");
				case "quest":
					return getListOfStringsMatchingLastWord(args, "set", "get");
				case "npc":
				case "gui":
					return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
				default:
					return Collections.emptyList();
			}
		}
		if (args.length == 3 && args[0].equals("npc")) {
			return getListOfStringsMatchingLastWord(args, "name", "relevance", "targetable", "path", "dialogue");
		}
		if (args.length == 4 && args[0].equals("npc")) {
			if (args[2].equals("relevance")) {
				return getListOfStringsMatchingLastWord(args, "none", "indirect", "main");
			}
			if (args[2].equals("targetable")) {
				return getListOfStringsMatchingLastWord(args, "true", "false");
			}
		}
		return Collections.emptyList();
	}
}
