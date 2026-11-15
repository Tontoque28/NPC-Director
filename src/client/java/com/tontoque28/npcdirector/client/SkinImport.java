package com.tontoque28.npcdirector.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import com.tontoque28.npcdirector.network.ImportSkinPayload;
import com.tontoque28.npcdirector.skin.SkinPng;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class SkinImport {

	public record Outcome(boolean sent, Component message) {
	}

	private SkinImport() {
	}

	private static SDL_DialogFileCallback activeCallback;

	public static void pickAsync(Runnable before, Consumer<String> after) {
		String title = ClientLang.tr("npcdirector.gui.skin_import.dialog").getString();
		String description = ClientLang.tr("npcdirector.gui.skin_import.filter").getString();
		before.run();
		Minecraft client = Minecraft.getInstance();
		try (MemoryStack stack = MemoryStack.stackPush()) {
			SDL_DialogFileFilter.Buffer filters = SDL_DialogFileFilter.malloc(1, stack);
			filters.get(0).set(stack.UTF8(description), stack.UTF8("png"));
			SDL_DialogFileCallback callback = SDL_DialogFileCallback.create((userdata, filelist, filter) -> {
				String chosen = null;
				if (filelist != 0L) {
					long first = MemoryUtil.memGetAddress(filelist);
					if (first != 0L) {
						chosen = MemoryUtil.memUTF8(first);
					}
				}
				String result = chosen;
				client.execute(() -> after.accept(result));
			});
			activeCallback = callback;
			SDLDialog.SDL_ShowOpenFileDialog(callback, 0L, 0L, filters, (CharSequence) null, false);
		} catch (Throwable throwable) {
			client.execute(() -> after.accept(null));
		}
	}

	private static Outcome refused(String key, Object... args) {
		return new Outcome(false, ClientLang.tr(key, args));
	}

	public static Outcome send(int npcEntityId, String chosen) {
		Path file;
		byte[] data;
		try {
			file = Path.of(chosen);
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
