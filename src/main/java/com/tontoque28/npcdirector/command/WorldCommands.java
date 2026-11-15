package com.tontoque28.npcdirector.command;

import com.tontoque28.npcdirector.util.Texts;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.tontoque28.npcdirector.util.DebugLog;
import com.tontoque28.npcdirector.world.WorldContent;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.ITextComponent;

public class WorldCommands {

	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(Commands.literal("npcdirector")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("world")
						.then(Commands.literal("list").executes(WorldCommands::list))
						.then(Commands.literal("path")
								.then(Commands.argument("name", StringArgumentType.word())
										.executes(ctx -> show(ctx, true))
										.then(Commands.literal("remove").executes(ctx -> remove(ctx, true)))))
						.then(Commands.literal("dialogue")
								.then(Commands.argument("name", StringArgumentType.word())
										.executes(ctx -> show(ctx, false))
										.then(Commands.literal("remove").executes(ctx -> remove(ctx, false)))))));
	}

	private static int list(CommandContext<CommandSource> ctx) {
		WorldContent content = WorldContent.get(ctx.getSource().getServer());
		int paths = content.pathCount();
		int dialogues = content.dialogueCount();
		ctx.getSource().sendSuccess(Texts.literal("[NPCDirector] Datos del mundo: caminos=" + paths + " dialogos=" + dialogues), false);
		return 1;
	}

	private static int show(CommandContext<CommandSource> ctx, boolean path) {
		String name = StringArgumentType.getString(ctx, "name");
		WorldContent content = WorldContent.get(ctx.getSource().getServer());
		String json = path ? content.path(name) : content.dialogue(name);
		if (json == null) {
			ctx.getSource().sendFailure(Texts.literal("[NPCDirector] No existe mundo:" + name));
			return 0;
		}
		ctx.getSource().sendSuccess(Texts.literal(json), false);
		return 1;
	}

	private static int remove(CommandContext<CommandSource> ctx, boolean path) {
		String name = StringArgumentType.getString(ctx, "name");
		WorldContent content = WorldContent.get(ctx.getSource().getServer());
		boolean removed = path ? content.removePath(name) : content.removeDialogue(name);
		if (!removed) {
			ctx.getSource().sendFailure(Texts.literal("[NPCDirector] No existe mundo:" + name));
			return 0;
		}
		DebugLog.action("world remove " + name);
		ctx.getSource().sendSuccess(Texts.literal("[NPCDirector] Eliminado mundo:" + name), true);
		return 1;
	}
}
