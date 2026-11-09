package com.tontoque28.npcdirector.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.config.ModConfig;
import com.tontoque28.npcdirector.data.NpcProfile;
import com.tontoque28.npcdirector.data.QuestRelevance;
import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.util.DebugLog;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public class DevCommands {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("npcdirector")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("mode")
						.then(Commands.literal("dev").executes(ctx -> setMode(ctx.getSource(), true)))
						.then(Commands.literal("player").executes(ctx -> setMode(ctx.getSource(), false))))
				.then(Commands.literal("debug")
						.then(Commands.literal("on").executes(ctx -> setDebug(ctx.getSource(), true)))
						.then(Commands.literal("off").executes(ctx -> setDebug(ctx.getSource(), false)))
						.then(Commands.literal("log").executes(DevCommands::showLog))
						.then(Commands.literal("clear").executes(DevCommands::clearLog)))
				.then(Commands.literal("gui")
						.executes(DevCommands::guiLooked)
						.then(Commands.argument("target", EntityArgument.entity())
								.executes(DevCommands::guiTarget)))
				.then(Commands.literal("npc")
						.then(Commands.argument("target", EntityArgument.entity())
								.then(Commands.literal("name")
										.then(Commands.argument("value", StringArgumentType.greedyString())
												.executes(ctx -> npcName(ctx.getSource(),
														EntityArgument.getEntity(ctx, "target"),
														StringArgumentType.getString(ctx, "value")))))
								.then(Commands.literal("relevance")
										.then(Commands.literal("none").executes(ctx -> npcRelevance(ctx.getSource(), EntityArgument.getEntity(ctx, "target"), QuestRelevance.NONE)))
										.then(Commands.literal("indirect").executes(ctx -> npcRelevance(ctx.getSource(), EntityArgument.getEntity(ctx, "target"), QuestRelevance.INDIRECT)))
										.then(Commands.literal("main").executes(ctx -> npcRelevance(ctx.getSource(), EntityArgument.getEntity(ctx, "target"), QuestRelevance.MAIN))))
								.then(Commands.literal("targetable")
										.then(Commands.argument("value", BoolArgumentType.bool())
												.executes(ctx -> npcTargetable(ctx.getSource(),
														EntityArgument.getEntity(ctx, "target"),
														BoolArgumentType.getBool(ctx, "value")))))
								.then(Commands.literal("path")
										.then(Commands.argument("value", StringArgumentType.greedyString())
												.executes(ctx -> npcPath(ctx.getSource(),
														EntityArgument.getEntity(ctx, "target"),
														StringArgumentType.getString(ctx, "value")))))
								.then(Commands.literal("dialogue")
										.then(Commands.argument("value", StringArgumentType.greedyString())
												.executes(ctx -> npcDialogue(ctx.getSource(),
														EntityArgument.getEntity(ctx, "target"),
														StringArgumentType.getString(ctx, "value")))))))
				.then(Commands.literal("quest")
						.then(Commands.literal("set")
								.then(Commands.argument("questId", StringArgumentType.string())
										.then(Commands.argument("state", StringArgumentType.string())
												.executes(ctx -> questSetGlobal(ctx.getSource(),
														StringArgumentType.getString(ctx, "questId"),
														StringArgumentType.getString(ctx, "state")))
												.then(Commands.argument("player", EntityArgument.player())
														.executes(ctx -> questSetPlayer(ctx.getSource(),
																EntityArgument.getPlayer(ctx, "player"),
																StringArgumentType.getString(ctx, "questId"),
																StringArgumentType.getString(ctx, "state")))))))
						.then(Commands.literal("get")
								.then(Commands.argument("questId", StringArgumentType.string())
										.executes(ctx -> questGetGlobal(ctx.getSource(),
												StringArgumentType.getString(ctx, "questId")))
										.then(Commands.argument("player", EntityArgument.player())
												.executes(ctx -> questGetPlayer(ctx.getSource(),
														EntityArgument.getPlayer(ctx, "player"),
														StringArgumentType.getString(ctx, "questId")))))))
		);
	}

	private static int guiLooked(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
		QuestNpcEntity npc = AdminService.lookedAt(player);
		if (npc == null) {
			AdminService.openSummonPanel(player);
			ctx.getSource().sendFailure(Component.literal("[NPCDirector] No hay ningun NPC a la vista a menos de 6 bloques"));
			return 0;
		}
		AdminService.openFor(player, npc);
		return 1;
	}

	private static int guiTarget(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
		Entity entity = EntityArgument.getEntity(ctx, "target");
		QuestNpcEntity npc = asNpc(ctx.getSource(), entity);
		if (npc == null) {
			return 0;
		}
		AdminService.openFor(player, npc);
		return 1;
	}

	private static com.tontoque28.npcdirector.quest.QuestManager questManager(CommandSourceStack source) {
		return com.tontoque28.npcdirector.quest.QuestManager.get(source.getLevel());
	}

	private static int questSetGlobal(CommandSourceStack source, String questId, String state) {
		questManager(source).setGlobalState(questId, state);
		source.sendSuccess(() -> Component.literal("[NPCDirector] Quest global " + questId + " = " + state), true);
		DebugLog.action("quest global set: " + questId + "=" + state);
		return 1;
	}

	private static int questSetPlayer(CommandSourceStack source, net.minecraft.server.level.ServerPlayer player, String questId, String state) {
		questManager(source).setPlayerState(player.getUUID(), questId, state);
		source.sendSuccess(() -> Component.literal("[NPCDirector] Quest " + questId + " = " + state + " para " + player.getName().getString()), true);
		DebugLog.action("quest player set: " + player.getName().getString() + " " + questId + "=" + state);
		return 1;
	}

	private static int questGetGlobal(CommandSourceStack source, String questId) {
		String state = questManager(source).getGlobalState(questId);
		source.sendSuccess(() -> Component.literal("[NPCDirector] " + questId + " (global) = " + state), false);
		return 1;
	}

	private static int questGetPlayer(CommandSourceStack source, net.minecraft.server.level.ServerPlayer player, String questId) {
		String state = questManager(source).getPlayerState(player.getUUID(), questId);
		source.sendSuccess(() -> Component.literal("[NPCDirector] " + questId + " (" + player.getName().getString() + ") = " + state), false);
		return 1;
	}

	private static QuestNpcEntity asNpc(CommandSourceStack source, Entity entity) {
		if (!(entity instanceof QuestNpcEntity npc)) {
			source.sendFailure(Component.literal("[NPCDirector] El objetivo no es un NPC de este mod"));
			return null;
		}
		return npc;
	}

	private static int npcName(CommandSourceStack source, Entity entity, String value) {
		QuestNpcEntity npc = asNpc(source, entity);
		if (npc == null) return 0;
		NpcProfile profile = npc.getProfile();
		profile.setDisplayName(value);
		npc.setProfile(profile);
		source.sendSuccess(() -> Component.literal("[NPCDirector] Nombre: " + value), true);
		DebugLog.action("npc name set: " + value);
		return 1;
	}

	private static int npcRelevance(CommandSourceStack source, Entity entity, QuestRelevance relevance) {
		QuestNpcEntity npc = asNpc(source, entity);
		if (npc == null) return 0;
		NpcProfile profile = npc.getProfile();
		profile.setQuestRelevance(relevance);
		npc.setProfile(profile);
		source.sendSuccess(() -> Component.literal("[NPCDirector] Relevancia: " + relevance), true);
		DebugLog.action("npc relevance set: " + relevance);
		return 1;
	}

	private static int npcTargetable(CommandSourceStack source, Entity entity, boolean value) {
		QuestNpcEntity npc = asNpc(source, entity);
		if (npc == null) return 0;
		npc.getProfile().setTargetableByHostiles(value);
		source.sendSuccess(() -> Component.literal("[NPCDirector] Targetable por hostiles: " + value), true);
		DebugLog.action("npc targetable set: " + value);
		return 1;
	}

	private static int npcPath(CommandSourceStack source, Entity entity, String value) {
		QuestNpcEntity npc = asNpc(source, entity);
		if (npc == null) return 0;
		npc.getProfile().setAssignedPathId(value);
		npc.setPatrolIndex(0);
		npc.setPatrolIdleTicks(-1);
		source.sendSuccess(() -> Component.literal("[NPCDirector] Camino asignado: " + value), true);
		if (WaypointPathManager.get(value) == null) {
			source.sendFailure(Component.literal("[NPCDirector] Aviso: el camino " + value + " no esta cargado"));
		}
		DebugLog.action("npc path set: " + value);
		return 1;
	}

	private static int npcDialogue(CommandSourceStack source, Entity entity, String value) {
		QuestNpcEntity npc = asNpc(source, entity);
		if (npc == null) return 0;
		npc.getProfile().setAssignedDialogueId(value);
		source.sendSuccess(() -> Component.literal("[NPCDirector] Diálogo asignado: " + value), true);
		if (DialogueManager.get(value) == null) {
			source.sendFailure(Component.literal("[NPCDirector] Aviso: el dialogo " + value + " no esta cargado"));
		}
		DebugLog.action("npc dialogue set: " + value);
		return 1;
	}

	private static int setMode(CommandSourceStack source, boolean dev) {
		ModConfig.setDevMode(dev);
		source.sendSuccess(() -> Component.literal("[NPCDirector] Modo: " + (dev ? "developer" : "player")), true);
		DebugLog.action("mode set to " + (dev ? "dev" : "player"));
		return 1;
	}

	private static int setDebug(CommandSourceStack source, boolean enabled) {
		DebugLog.setEnabled(enabled);
		source.sendSuccess(() -> Component.literal("[NPCDirector] Debug log: " + enabled), true);
		return 1;
	}

	private static int showLog(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
		DebugLog.recent(20).forEach(line -> ctx.getSource().sendSuccess(() -> Component.literal(line), false));
		return 1;
	}

	private static int clearLog(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
		DebugLog.clear();
		ctx.getSource().sendSuccess(() -> Component.literal("[NPCDirector] Log limpiado"), true);
		return 1;
	}
}

