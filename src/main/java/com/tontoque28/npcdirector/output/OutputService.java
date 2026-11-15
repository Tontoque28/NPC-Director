package com.tontoque28.npcdirector.output;

import com.tontoque28.npcdirector.admin.AdminService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.tontoque28.npcdirector.config.ModConfig;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.network.OutputLinesPayload;
import com.tontoque28.npcdirector.util.DebugLog;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class OutputService {

	public static final int MAX_HISTORY = 100;
	public static final int MAX_LINE = 240;
	public static final int MAX_BATCH = 40;

	private static final Deque<String> HISTORY = new ArrayDeque<>();
	private static final List<String> PENDING = new ArrayList<>();
	private static final Set<UUID> SUBSCRIBERS = new HashSet<>();

	private OutputService() {
	}

	public static String clean(String raw) {
		if (raw == null) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < raw.length() && builder.length() < MAX_LINE; i++) {
			char c = raw.charAt(i);
			builder.append(c < 32 || c == 127 || c == '§' ? ' ' : c);
		}
		return builder.toString().trim();
	}

	public static void line(String raw) {
		String text = clean(raw);
		if (text.isEmpty()) {
			return;
		}
		HISTORY.addLast(text);
		while (HISTORY.size() > MAX_HISTORY) {
			HISTORY.removeFirst();
		}
		if (!SUBSCRIBERS.isEmpty()) {
			PENDING.add(text);
			if (PENDING.size() > MAX_HISTORY) {
				PENDING.remove(0);
			}
		}
	}

	public static void subscribe(ServerPlayer player, boolean on) {
		if (!on) {
			SUBSCRIBERS.remove(player.getUUID());
			return;
		}
		if (!AdminService.isOperator(player)) {
			DebugLog.denied("panel de salida denegado por permisos: " + player.getName().getString());
			return;
		}
		if (!ModConfig.isDevMode()) {
			SUBSCRIBERS.remove(player.getUUID());
			NetworkingInit.sendToPlayer(player, new OutputLinesPayload(List.of("[NPCDirector] Modo dev desactivado. Usa /npcdirector mode dev")));
			DebugLog.denied("panel de salida sin modo dev: " + player.getName().getString());
			return;
		}
		SUBSCRIBERS.add(player.getUUID());
		List<String> recent = new ArrayList<>(HISTORY);
		int from = Math.max(0, recent.size() - MAX_BATCH);
		NetworkingInit.sendToPlayer(player, new OutputLinesPayload(new ArrayList<>(recent.subList(from, recent.size()))));
	}

	public static void remove(UUID id) {
		SUBSCRIBERS.remove(id);
	}

	public static boolean subscribed(UUID id) {
		return SUBSCRIBERS.contains(id);
	}

	public static void clearAll() {
		SUBSCRIBERS.clear();
		PENDING.clear();
		HISTORY.clear();
	}

	public static void flush(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		List<String> batch = new ArrayList<>(PENDING.subList(0, Math.min(PENDING.size(), MAX_BATCH)));
		PENDING.subList(0, batch.size()).clear();
		for (UUID id : new ArrayList<>(SUBSCRIBERS)) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null || !AdminService.isOperator(player) || !ModConfig.isDevMode()) {
				SUBSCRIBERS.remove(id);
				continue;
			}
			NetworkingInit.sendToPlayer(player, new OutputLinesPayload(batch));
		}
	}
}
