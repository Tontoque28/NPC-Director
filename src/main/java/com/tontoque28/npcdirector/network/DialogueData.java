package com.tontoque28.npcdirector.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;

public record DialogueData(List<Node> nodes) {

	public static final int HARD_NODES = 128;
	public static final int HARD_OPTIONS = 256;
	public static final int HARD_COMMANDS = 128;
	public static final int WIRE_STRING = 32767;
	public static final int SAFE_BYTES = 30000;

	public record Opt(String text, List<String> commands, String next, String questId, String questState) {
	}

	public record Node(String id, String text, List<Opt> options) {
	}

	public static DialogueData empty() {
		return new DialogueData(new ArrayList<>());
	}

	public int estimateBytes() {
		int total = 8;
		for (Node node : nodes) {
			total += 8 + utf(node.id()) + utf(node.text());
			for (Opt opt : node.options()) {
				total += 12 + utf(opt.text()) + utf(opt.next()) + utf(opt.questId()) + utf(opt.questState());
				for (String command : opt.commands()) {
					total += 4 + utf(command);
				}
			}
		}
		return total;
	}

	private static int utf(String text) {
		return text == null ? 0 : text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(nodes.size());
		for (Node node : nodes) {
			buf.writeUtf(node.id(), WIRE_STRING);
			buf.writeUtf(node.text(), WIRE_STRING);
			buf.writeVarInt(node.options().size());
			for (Opt opt : node.options()) {
				buf.writeUtf(opt.text(), WIRE_STRING);
				buf.writeVarInt(opt.commands().size());
				for (String command : opt.commands()) {
					buf.writeUtf(command, WIRE_STRING);
				}
				buf.writeUtf(opt.next(), WIRE_STRING);
				buf.writeUtf(opt.questId(), WIRE_STRING);
				buf.writeUtf(opt.questState(), WIRE_STRING);
			}
		}
	}

	public static DialogueData read(FriendlyByteBuf buf) {
		int nodeCount = buf.readVarInt();
		if (nodeCount < 0 || nodeCount > HARD_NODES) {
			throw new IllegalArgumentException("demasiados nodos");
		}
		List<Node> nodes = new ArrayList<>(nodeCount);
		for (int n = 0; n < nodeCount; n++) {
			String id = buf.readUtf(WIRE_STRING);
			String text = buf.readUtf(WIRE_STRING);
			int optionCount = buf.readVarInt();
			if (optionCount < 0 || optionCount > HARD_OPTIONS) {
				throw new IllegalArgumentException("demasiadas opciones");
			}
			List<Opt> options = new ArrayList<>(optionCount);
			for (int o = 0; o < optionCount; o++) {
				String optText = buf.readUtf(WIRE_STRING);
				int commandCount = buf.readVarInt();
				if (commandCount < 0 || commandCount > HARD_COMMANDS) {
					throw new IllegalArgumentException("demasiados comandos");
				}
				List<String> commands = new ArrayList<>(commandCount);
				for (int c = 0; c < commandCount; c++) {
					commands.add(buf.readUtf(WIRE_STRING));
				}
				String next = buf.readUtf(WIRE_STRING);
				String questId = buf.readUtf(WIRE_STRING);
				String questState = buf.readUtf(WIRE_STRING);
				options.add(new Opt(optText, commands, next, questId, questState));
			}
			nodes.add(new Node(id, text, options));
		}
		return new DialogueData(nodes);
	}
}
