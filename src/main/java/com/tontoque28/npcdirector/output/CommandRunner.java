package com.tontoque28.npcdirector.output;

import com.tontoque28.npcdirector.util.Texts;
import java.util.ArrayList;
import java.util.List;

import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.util.DebugLog;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

public final class CommandRunner {

	private static final TextColor RED = TextColor.fromLegacyFormat(ChatFormatting.RED);

	private CommandRunner() {
	}

	public static boolean run(ServerPlayer player, String raw, String origin) {
		String command = raw == null ? "" : raw.trim();
		if (command.startsWith("/")) {
			command = command.substring(1).trim();
		}
		if (command.isEmpty()) {
			return true;
		}
		final String shown = "/" + command;
		List<String> messages = new ArrayList<>();
		boolean[] failed = {false};
		CommandSource sink = new CommandSource() {
			@Override
			public void sendMessage(Component message, java.util.UUID sender) {
				messages.add(message.getString());
				boolean red = RED.equals(message.getStyle().getColor());
				if (red) {
					failed[0] = true;
					Texts.tell(player, Texts.literal("[NPCDirector] El comando fallo: ").append(message));
				}
			}

			@Override
			public boolean acceptsSuccess() {
				return true;
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
		CommandSourceStack source = new CommandSourceStack(sink, player.position(), player.getRotationVector(), player.getLevel(), 4,
				player.getName().getString(), player.getDisplayName(), player.getServer(), player);
		try {
			player.getServer().getCommands().performCommand(source, command);
		} catch (Exception exception) {
			failed[0] = true;
			String reason = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
			messages.add(reason);
			DebugLog.error("comando fallo: " + shown + " - " + reason);
			NpcDirector.LOGGER.error("Dialogue command failed: {}", shown, exception);
			Texts.tell(player, Texts.literal("[NPCDirector] El comando fallo: " + shown));
		}
		StringBuilder line = new StringBuilder("[").append(origin).append("] ").append(failed[0] ? "FALLO " : "OK ").append(shown);
		if (!messages.isEmpty()) {
			line.append(" -> ").append(String.join(" | ", messages));
		}
		OutputService.line(line.toString());
		DebugLog.action("comando " + (failed[0] ? "fallo" : "ok") + " (" + origin + "): " + shown);
		return !failed[0];
	}
}
