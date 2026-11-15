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
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
		NpcSettingCommands.register(dispatcher);
		WorldCommands.register(dispatcher);
	}

	private static int guiLooked(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
		if (!devRequired(ctx.getSource())) {
			return 0;
		}
		QuestNpcEntity npc = AdminService.lookedAt(player);
		if (npc == null) {
			AdminService.openSummonPanel(player);
			ctx.getSource().sendFailure(Component.translatableWithFallback("npcdirector.msg.no_npc_near", "[NPCDirector] No NPC in sight within 6 blocks"));
			return 0;
		}
		AdminService.openFor(player, npc);
		return 1;
	}

	private static int guiTarget(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
		if (!devRequired(ctx.getSource())) {
			return 0;
		}
		Entity entity = EntityArgument.getEntity(ctx, "target");
		QuestNpcEntity npc = asNpc(ctx.getSource(), entity);
		if (npc == null) {
			return 0;
		}
		AdminService.openFor(player, npc);
		return 1;
	}

	private static boolean devRequired(CommandSourceStack source) {
		if (ModConfig.isDevMode()) {
			return true;
		}
		source.sendFailure(Component.translatableWithFallback("npcdirector.msg.dev_required", "[NPCDirector] The edit panel is only available in developer mode. Use /npcdirector mode dev"));
		return false;
	}

	private static com.tontoque28.npcdirector.quest.QuestManager questManager(CommandSourceStack source) {
		return com.tontoque28.npcdirector.quest.QuestManager.get(source.getLevel());
	}

	private static int questSetGlobal(CommandSourceStack source, String questId, String state) {
		questManager(source).setGlobalState(questId, state);
		source.sendSuccess(() -> Component.translatableWithFallback("npcdirector.msg.quest.set_global", "[NPCDirector] Global quest %s = %s", questId, state), true);
		DebugLog.action("quest global set: " + questId + "=" + state);
		return 1;
	}

	private static int questSetPlayer(CommandSourceStack source, net.minecraft.server.level.ServerPlayer player, String questId, String state) {
		questManager(source).setPlayerState(player.getUUID(), questId, state);
		source.sendSuccess(() -> Component.translatableWithFallback("npcdirector.msg.quest.set_player", "[NPCDirector] Quest %s = %s for %s", questId, state, player.getName().getString()), true);
		DebugLog.action("quest player set: " + player.getName().getString() + " " + questId + "=" + state);
		return 1;
	}

	private static int questGetGlobal(CommandSourceStack source, String questId) {
		String state = questManager(source).getGlobalState(questId);
		source.sendSuccess(() -> Component.translatableWithFallback("npcdirector.msg.quest.get_global", "[NPCDirector] %s (global) = %s", questId, state), false);
		return 1;
	}

	private static int questGetPlayer(CommandSourceStack source, net.minecraft.server.level.ServerPlayer player, String questId) {
		String state = questManager(source).getPlayerState(player.getUUID(), questId);
		source.sendSuccess(() -> Component.translatableWithFallback("npcdirector.msg.quest.get_player", "[NPCDirector] %s (%s) = %s", questId, player.getName().getString(), state), false);
		return 1;
	}

	private static QuestNpcEntity asNpc(CommandSourceStack source, Entity entity) {
		if (!(entity instanceof QuestNpcEntity npc)) {
			source.sendFailure(Component.translatableWithFallback("npcdirector.msg.not_npc", "[NPCDirector] The target is not an NPC of this mod"));
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
		source.sendSuccess(() -> Component.translatableWithFallback("npcdirector.msg.npc.name", "[NPCDirector] Name: %s", value), true);
		DebugLog.action("npc name set: " + value);
		return 1;
	}

	private static int npcRelevance(CommandSourceStack source, Entity entity, QuestRelevance relevance) {
		QuestNpcEntity npc = asNpc(source, entity);
		if (npc == null) return 0;
		NpcProfile profile = npc.getProfile();
		profile.setQuestRelevance(relevance);
		npc.setProfile(profile);
		source.sendSuccess(() -> Component.translatableWithFallback("npcdirector.msg.npc.relevance", "[NPCDirector] Relevance: %s", String.valueOf(relevance)), true);
		DebugLog.action("npc relevance set: " + relevance);
		return 1;
	}

	private static int npcTargetable(CommandSourceStack source, Entity entity, boolean value) {
		QuestNpcEntity npc = asNpc(source, entity);
		if (npc == null) return 0;
		npc.getProfile().setTargetableByHostiles(value);
		source.sendSuccess(() -> Component.translatableWithFallback("npcdirector.msg.npc.targetable", "[NPCDirector] Targetable by hostiles: %s", String.valueOf(value)), true);
		DebugLog.action("npc targetable set: " + value);
		return 1;
	}

	private static int npcPath(CommandSourceStack source, Entity entity, String value) {
		QuestNpcEntity npc = asNpc(source, entity);
		if (npc == null) return 0;
		npc.getProfile().setAssignedPathId(value);
		npc.setPatrolIndex(0);
		npc.setPatrolIdleTicks(-1);
		source.sendSuccess(() -> Component.translatableWithFallback("npcdirector.msg.npc.path_set", "[NPCDirector] Path assigned: %s", value), true);
		if (WaypointPathManager.get(value) == null) {
			source.sendFailure(Component.translatableWithFallback("npcdirector.msg.npc.path_missing", "[NPCDirector] Warning: path %s is not loaded", value));
		}
		DebugLog.action("npc path set: " + value);
		return 1;
	}

	private static int npcDialogue(CommandSourceStack source, Entity entity, String value) {
		QuestNpcEntity npc = asNpc(source, entity);
		if (npc == null) return 0;
		npc.getProfile().setAssignedDialogueId(value);
		source.sendSuccess(() -> Component.translatableWithFallback("npcdirector.msg.npc.dialogue_set", "[NPCDirector] Dialogue assigned: %s", value), true);
		if (DialogueManager.get(value) == null) {
			source.sendFailure(Component.translatableWithFallback("npcdirector.msg.npc.dialogue_missing", "[NPCDirector] Warning: dialogue %s is not loaded", value));
		}
		DebugLog.action("npc dialogue set: " + value);
		return 1;
	}

	private static int setMode(CommandSourceStack source, boolean dev) {
		ModConfig.setDevMode(dev);
		source.sendSuccess(() -> Component.translatableWithFallback("npcdirector.msg.mode", "[NPCDirector] Mode: %s", (dev ? "developer" : "player")), true);
		DebugLog.action("mode set to " + (dev ? "dev" : "player"));
		return 1;
	}

	private static int setDebug(CommandSourceStack source, boolean enabled) {
		DebugLog.setEnabled(enabled);
		source.sendSuccess(() -> Component.translatableWithFallback("npcdirector.msg.debug", "[NPCDirector] Debug log: %s", String.valueOf(enabled)), true);
		return 1;
	}

	private static int showLog(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
		DebugLog.recent(20).forEach(line -> ctx.getSource().sendSuccess(() -> Component.literal(line), false));
		return 1;
	}

	private static int clearLog(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
		DebugLog.clear();
		ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("npcdirector.msg.log.cleared", "[NPCDirector] Log cleared"), true);
		return 1;
	}
}

