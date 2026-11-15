package com.tontoque28.npcdirector.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import com.tontoque28.npcdirector.network.ImportSkinPayload;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.skin.SkinPng;

import net.minecraft.client.Minecraft;

public final class SkinImport {

	private static final AtomicInteger SEND_IDS = new AtomicInteger(1);
	private static volatile boolean picking = false;

	private SkinImport() {
	}

	public static void pick(int npcEntityId) {
		if (picking) {
			return;
		}
		picking = true;
		Thread thread = new Thread(() -> {
			try {
				File file = choose();
				if (file != null) {
					load(npcEntityId, file);
				}
			} catch (Throwable error) {
				message("npcdirector.skin.import_file_bad");
			} finally {
				picking = false;
			}
		}, "NPCDirector-skin-picker");
		thread.setDaemon(true);
		thread.start();
	}

	private static File choose() {
		java.awt.FileDialog dialog = new java.awt.FileDialog((java.awt.Frame) null, "PNG 64x64 / 64x32", java.awt.FileDialog.LOAD);
		dialog.setFilenameFilter((dir, name) -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".png"));
		dialog.setFile("*.png");
		dialog.setVisible(true);
		String name = dialog.getFile();
		String dir = dialog.getDirectory();
		dialog.dispose();
		if (name == null || dir == null) {
			return null;
		}
		return new File(dir, name);
	}

	private static void load(int npcEntityId, File file) {
		byte[] data;
		try {
			if (file.length() > SkinPng.MAX_BYTES) {
				message("npcdirector.skin.import_big");
				return;
			}
			data = Files.readAllBytes(file.toPath());
		} catch (IOException exception) {
			message("npcdirector.skin.import_file_bad");
			return;
		}
		SkinPng.Problem problem = SkinPng.check(data);
		if (problem != SkinPng.Problem.NONE) {
			message(problem == SkinPng.Problem.DIMENSIONS ? "npcdirector.skin.import_size" : problem == SkinPng.Problem.TOO_BIG ? "npcdirector.skin.import_big" : "npcdirector.skin.import_bad");
			return;
		}
		int chunk = ImportSkinPayload.CHUNK_MAX;
		int total = (data.length + chunk - 1) / chunk;
		int sendId = SEND_IDS.getAndIncrement();
		Minecraft minecraft = Minecraft.getMinecraft();
		minecraft.addScheduledTask(() -> {
			for (int i = 0; i < total; i++) {
				byte[] part = Arrays.copyOfRange(data, i * chunk, Math.min(data.length, (i + 1) * chunk));
				NetworkingInit.sendToServer(new ImportSkinPayload(npcEntityId, sendId, i, total, data.length, part));
			}
		});
		message("npcdirector.skin.import_sent");
	}

	public static void clear(int npcEntityId) {
		NetworkingInit.sendToServer(new ImportSkinPayload(npcEntityId, SEND_IDS.getAndIncrement(), 0, 1, 0, new byte[0]));
	}

	private static void message(String key) {
		ClientPacketHandler.panelMessage(key);
	}
}
