package com.tontoque28.npcdirector.util;

import com.tontoque28.npcdirector.NpcDirector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ArrayList;

public class DebugLog {

	private static final int MAX_ENTRIES = 200;
	private static final Deque<String> ENTRIES = new ArrayDeque<>();
	private static boolean enabled = false;

	public static void setEnabled(boolean value) {
		enabled = value;
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void action(String message) {
		record("ACTION", message);
	}

	public static void error(String message) {
		record("ERROR", message);
		NpcDirector.LOGGER.error("[NPCDirector] {}", message);
	}

	public static void denied(String message) {
		record("DENIED", message);
		NpcDirector.LOGGER.warn("[NPCDirector] {}", message);
	}

	private static void record(String tag, String message) {
		if (!enabled) {
			return;
		}
		String entry = "[" + tag + "] " + message;
		ENTRIES.addLast(entry);
		while (ENTRIES.size() > MAX_ENTRIES) {
			ENTRIES.removeFirst();
		}
	}

	public static List<String> recent(int count) {
		List<String> list = new ArrayList<>(ENTRIES);
		int from = Math.max(0, list.size() - count);
		return list.subList(from, list.size());
	}

	public static void clear() {
		ENTRIES.clear();
	}
}
