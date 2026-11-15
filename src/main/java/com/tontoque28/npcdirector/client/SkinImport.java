package com.tontoque28.npcdirector.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import com.tontoque28.npcdirector.network.ImportSkinPayload;
import com.tontoque28.npcdirector.skin.SkinPng;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;

public final class SkinImport {

	public static final class Outcome {
		private final boolean sent;
		private final ITextComponent message;

		Outcome(boolean sent, ITextComponent message) {
			this.sent = sent;
			this.message = message;
		}

		public boolean sent() {
			return sent;
		}

		public ITextComponent message() {
			return message;
		}
	}

	private SkinImport() {
	}

	public static String pick(String title, String description) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			PointerBuffer filters = stack.mallocPointer(1);
			filters.put(stack.UTF8("*.png")).flip();
			return TinyFileDialogs.tinyfd_openFileDialog(title, "", filters, description, false);
		}
	}

	public static void pickAsync(Runnable before, Consumer<String> after) {
		String title = ClientLang.tr("npcdirector.gui.skin_import.dialog").getString();
		String description = ClientLang.tr("npcdirector.gui.skin_import.filter").getString();
		before.run();
		Thread thread = new Thread(() -> {
			String chosen;
			try {
				chosen = pick(title, description);
			} catch (Throwable throwable) {
				chosen = null;
			}
			String result = chosen;
			Minecraft.getInstance().execute(() -> after.accept(result));
		}, "NPCDirector-file-dialog");
		thread.setDaemon(true);
		thread.start();
	}

	private static Outcome refused(String key, Object... args) {
		return new Outcome(false, ClientLang.tr(key, args));
	}

	public static Outcome send(int npcEntityId, String chosen) {
		Path file;
		byte[] data;
		try {
			file = Paths.get(chosen);
			if (Files.size(file) > SkinPng.MAX_BYTES) {
				return refused("npcdirector.gui.skin_import.too_big");
			}
			data = Files.readAllBytes(file);
		} catch (IOException | RuntimeException exception) {
			return refused("npcdirector.gui.skin_import.unreadable");
		}
		SkinPng.Problem problem = SkinPng.check(data);
		if (problem == SkinPng.Problem.TOO_BIG) {
			return refused("npcdirector.gui.skin_import.too_big");
		}
		if (problem == SkinPng.Problem.DIMENSIONS) {
			return refused("npcdirector.gui.skin_import.bad_size");
		}
		if (problem != SkinPng.Problem.NONE) {
			return refused("npcdirector.gui.skin_import.not_png");
		}
		int upload = ThreadLocalRandom.current().nextInt(1, 1 << 20);
		int total = (data.length + ImportSkinPayload.MAX_CHUNK - 1) / ImportSkinPayload.MAX_CHUNK;
		for (int i = 0; i < total; i++) {
			int from = i * ImportSkinPayload.MAX_CHUNK;
			int to = Math.min(data.length, from + ImportSkinPayload.MAX_CHUNK);
			ClientNet.send(new ImportSkinPayload(npcEntityId, upload, i, total, data.length, java.util.Arrays.copyOfRange(data, from, to)));
		}
		return new Outcome(true, ClientLang.tr("npcdirector.gui.skin_import.sent", file.getFileName().toString()));
	}
}
