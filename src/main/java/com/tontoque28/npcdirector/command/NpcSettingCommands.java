package com.tontoque28.npcdirector.command;

import com.tontoque28.npcdirector.util.Texts;
import java.util.function.Consumer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.data.NpcProfile;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.util.DebugLog;
import com.tontoque28.npcdirector.util.Validate;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class NpcSettingCommands {

	private static final String[] ARMS = {"auto", "wide", "slim"};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, net.minecraft.commands.arguments.selector.EntitySelector> target = Commands.argument("target", EntityArgument.entity());
		target.then(Commands.literal("skin")
				.then(Commands.argument("value", StringArgumentType.greedyString()).executes(NpcSettingCommands::skin)));
		com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> equip = Commands.literal("equip");
		for (int i = 0; i < QuestNpcEntity.EQUIP_NAMES.length; i++) {
			int index = i;
			equip.then(Commands.literal(QuestNpcEntity.EQUIP_NAMES[i])
					.then(Commands.argument("item", StringArgumentType.greedyString()).executes(ctx -> equip(ctx, index))));
		}
		target.then(equip);
		target.then(Commands.literal("scale")
				.then(Commands.argument("value", FloatArgumentType.floatArg(Validate.SCALE_MIN, Validate.SCALE_MAX))
						.executes(ctx -> edit(ctx, "scale", npc -> npc.getProfile().setScale(FloatArgumentType.getFloat(ctx, "value"))))));
		com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> arms = Commands.literal("arms");
		for (int i = 0; i < ARMS.length; i++) {
			int mode = i;
			arms.then(Commands.literal(ARMS[i]).executes(ctx -> edit(ctx, "arms " + ARMS[mode], npc -> npc.getProfile().setArmModel(mode))));
		}
		target.then(arms);
		target.then(bool("enabled", (profile, value) -> profile.setEnabled(value)));
		target.then(bool("showname", (profile, value) -> profile.setNameVisible(value)));
		target.then(Commands.literal("namecolor")
				.then(Commands.argument("value", IntegerArgumentType.integer(0, 15))
						.executes(ctx -> edit(ctx, "namecolor", npc -> npc.getProfile().setNameColor(IntegerArgumentType.getInteger(ctx, "value"))))));
		target.then(bool("look", (profile, value) -> profile.setLookAtPlayer(value)));
		target.then(bool("still", (profile, value) -> profile.setStationary(value)));
		target.then(bool("invulnerable", (profile, value) -> profile.setInvulnerable(value)));
		target.then(bool("sit", (profile, value) -> profile.setSitting(value)));
		target.then(Commands.literal("speed")
				.then(Commands.argument("value", FloatArgumentType.floatArg(Validate.SPEED_MIN, Validate.SPEED_MAX))
						.executes(ctx -> edit(ctx, "speed", npc -> npc.getProfile().setPatrolSpeed(FloatArgumentType.getFloat(ctx, "value"))))));
		target.then(Commands.literal("greeting")
				.then(Commands.argument("value", StringArgumentType.greedyString())
						.executes(ctx -> {
							String text = StringArgumentType.getString(ctx, "value");
							String clean = text.equalsIgnoreCase("none") ? "" : Validate.cleanText(text, Validate.GREETING_MAX);
							return edit(ctx, "greeting", npc -> npc.getProfile().setGreeting(clean));
						})));
		target.then(Commands.literal("greetrange")
				.then(Commands.argument("value", IntegerArgumentType.integer(1, 16))
						.executes(ctx -> edit(ctx, "greetrange", npc -> npc.getProfile().setGreetRange(IntegerArgumentType.getInteger(ctx, "value"))))));
		target.then(Commands.literal("sound")
				.then(Commands.literal("none").executes(ctx -> edit(ctx, "sound none", npc -> npc.getProfile().setSoundId(""))))
				.then(Commands.argument("id", ResourceLocationArgument.id())
						.executes(ctx -> sound(ctx, 1.0F, 1.0F))
						.then(Commands.argument("volume", FloatArgumentType.floatArg(0.0F, 2.0F))
								.executes(ctx -> sound(ctx, FloatArgumentType.getFloat(ctx, "volume"), 1.0F))
								.then(Commands.argument("pitch", FloatArgumentType.floatArg(0.5F, 2.0F))
										.executes(ctx -> sound(ctx, FloatArgumentType.getFloat(ctx, "volume"), FloatArgumentType.getFloat(ctx, "pitch")))))));
		dispatcher.register(Commands.literal("npcdirector")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("npc").then(target)));
	}

	private interface ProfileBool {
		void set(NpcProfile profile, boolean value);
	}

	private static ArgumentBuilder<CommandSourceStack, ?> bool(String name, ProfileBool setter) {
		return Commands.literal(name)
				.then(Commands.argument("value", BoolArgumentType.bool())
						.executes(ctx -> edit(ctx, name, npc -> setter.set(npc.getProfile(), BoolArgumentType.getBool(ctx, "value")))));
	}

	private static QuestNpcEntity npcOf(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		Entity entity = EntityArgument.getEntity(ctx, "target");
		if (!(entity instanceof QuestNpcEntity)) {
			ctx.getSource().sendFailure(Texts.literal("[NPCDirector] El objetivo no es un NPC de este mod"));
			return null;
		}
		QuestNpcEntity npc = (QuestNpcEntity) entity;
		return npc;
	}

	private static int edit(CommandContext<CommandSourceStack> ctx, String label, Consumer<QuestNpcEntity> change) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		QuestNpcEntity npc = npcOf(ctx);
		if (npc == null) {
			return 0;
		}
		change.accept(npc);
		npc.setProfile(npc.getProfile());
		ctx.getSource().sendSuccess(Texts.literal("[NPCDirector] " + label + " actualizado"), true);
		DebugLog.action("npc " + label + " por comando");
		return 1;
	}

	private static int sound(CommandContext<CommandSourceStack> ctx, float volume, float pitch) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		String id = ResourceLocationArgument.getId(ctx, "id").toString();
		if (Validate.sound(id) == null) {
			ctx.getSource().sendFailure(Texts.literal("[NPCDirector] Sonido inexistente: " + id));
			DebugLog.denied("comando sound con id inexistente: " + id);
			return 0;
		}
		return edit(ctx, "sound", npc -> {
			npc.getProfile().setSoundId(id);
			npc.getProfile().setSoundVolume(volume);
			npc.getProfile().setSoundPitch(pitch);
		});
	}

	private static int equip(CommandContext<CommandSourceStack> ctx, int index) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		QuestNpcEntity npc = npcOf(ctx);
		if (npc == null) {
			return 0;
		}
		String item = StringArgumentType.getString(ctx, "item");
		if (!AdminService.setEquip(npc, index, item)) {
			ctx.getSource().sendFailure(Texts.literal("[NPCDirector] Objeto inexistente: " + Validate.cleanText(item, 60)));
			DebugLog.denied("comando equip con objeto inexistente: " + Validate.cleanText(item, 60));
			return 0;
		}
		ctx.getSource().sendSuccess(Texts.literal("[NPCDirector] Equipo " + QuestNpcEntity.EQUIP_NAMES[index] + ": " + (Validate.isClear(item) ? "vacio" : item)), true);
		DebugLog.action("npc equip " + QuestNpcEntity.EQUIP_NAMES[index] + " por comando");
		return 1;
	}

	private static int skin(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		QuestNpcEntity npc = npcOf(ctx);
		if (npc == null) {
			return 0;
		}
		String name = StringArgumentType.getString(ctx, "value").trim();
		ServerPlayer player = ctx.getSource().getEntity() instanceof ServerPlayer ? (ServerPlayer) ctx.getSource().getEntity() : null;
		boolean ok = AdminService.startSkin(npc, name, AdminService.notifier(player, ctx.getSource().getServer()));
		if (!ok) {
			ctx.getSource().sendFailure(Texts.literal("[NPCDirector] Nombre de skin no valido (1 a 16 caracteres A-Z a-z 0-9 _)"));
			DebugLog.denied("comando skin con nombre no valido: " + Validate.cleanText(name, 40));
			return 0;
		}
		ctx.getSource().sendSuccess(Texts.literal("[NPCDirector] Skin: " + (name.equalsIgnoreCase("none") ? "por defecto" : name) + " (se resuelve en segundo plano)"), true);
		DebugLog.action("npc skin por comando: " + name);
		return 1;
	}
}
