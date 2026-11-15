package com.tontoque28.npcdirector.skin;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.network.ImportSkinPayload;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.network.SkinSyncPayload;
import com.tontoque28.npcdirector.output.OutputService;
import com.tontoque28.npcdirector.util.DebugLog;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CustomSkinService {

	public static final int MAX_CHUNKS = 8;
	public static final long COOLDOWN_TICKS = 40L;
	public static final long UPLOAD_TIMEOUT_TICKS = 600L;

	private static final class Upload {
		int npcId;
		int uploadId;
		int total;
		int totalBytes;
		int bytes;
		int received;
		long startTick;
		byte[][] parts;
	}

	private static final Map<UUID, Upload> UPLOADS = new HashMap<>();
	private static final Map<UUID, Long> LAST_START = new HashMap<>();

	private CustomSkinService() {
	}

	public static void forget(UUID player) {
		UPLOADS.remove(player);
		LAST_START.remove(player);
	}

	public static void clearAll() {
		UPLOADS.clear();
		LAST_START.clear();
	}

	private static void message(ServerPlayer player, String key, String fallback, Object... args) {
		AdminService.say(player, Component.translatableWithFallback(key, fallback, args));
	}

	private static void refuse(ServerPlayer player, String detail, String key, String fallback) {
		DebugLog.denied("importar skin rechazado de " + player.getName().getString() + ": " + detail);
		OutputService.line("[NPC] importar skin rechazado: " + detail);
		message(player, key, fallback);
	}

	private static void rejectChunk(ServerPlayer player, int index, String detail, String key, String fallback) {
		boolean active = UPLOADS.remove(player.getUUID()) != null;
		if (index == 0 || active) {
			refuse(player, detail, key, fallback);
		} else {
			DebugLog.denied("trozo de skin descartado de " + player.getName().getString() + ": " + detail);
		}
	}

	public static void receive(ServerPlayer player, ImportSkinPayload payload) {
		QuestNpcEntity npc = AdminService.validate(player, payload.npcEntityId(), "importar skin");
		if (npc == null) {
			UPLOADS.remove(player.getUUID());
			return;
		}
		UUID id = player.getUUID();
		long now = player.level().getGameTime();
		int total = payload.total();
		int index = payload.index();
		if (total < 1 || total > MAX_CHUNKS || index < 0 || index >= total || payload.data().length > ImportSkinPayload.MAX_CHUNK
				|| payload.totalBytes() < 1 || payload.data().length < 1) {
			rejectChunk(player, index, "parametros de troceado no validos", "npcdirector.skin.import.corrupt", "[NPCDirector] Skin file rejected: it is not a valid PNG");
			return;
		}
		if (payload.totalBytes() > SkinPng.MAX_BYTES) {
			rejectChunk(player, index, "tamano anunciado " + payload.totalBytes(), "npcdirector.skin.import.too_big", "[NPCDirector] Skin file rejected: it is larger than 16 KB");
			return;
		}
		Upload upload;
		if (index == 0) {
			Long last = LAST_START.get(id);
			if (last != null && now >= last && now - last < COOLDOWN_TICKS) {
				UPLOADS.remove(id);
				DebugLog.denied("importar skin demasiado rapido de " + player.getName().getString());
				message(player, "npcdirector.skin.import.too_fast", "[NPCDirector] Wait a moment before importing another skin");
				return;
			}
			LAST_START.put(id, now);
			if (LAST_START.size() > 64) {
				LAST_START.values().removeIf(time -> now - time >= COOLDOWN_TICKS);
			}
			upload = new Upload();
			upload.npcId = payload.npcEntityId();
			upload.uploadId = payload.uploadId();
			upload.total = total;
			upload.totalBytes = payload.totalBytes();
			upload.startTick = now;
			upload.parts = new byte[total][];
			UPLOADS.put(id, upload);
		} else {
			upload = UPLOADS.get(id);
			if (upload == null || upload.npcId != payload.npcEntityId() || upload.uploadId != payload.uploadId() || upload.total != total
					|| upload.totalBytes != payload.totalBytes() || upload.parts[index] != null) {
				UPLOADS.remove(id);
				DebugLog.denied("trozo de skin fuera de secuencia de " + player.getName().getString());
				return;
			}
			if (now - upload.startTick > UPLOAD_TIMEOUT_TICKS) {
				UPLOADS.remove(id);
				refuse(player, "importacion caducada", "npcdirector.skin.import.incomplete", "[NPCDirector] Skin import timed out, try again");
				return;
			}
		}
		upload.parts[index] = payload.data();
		upload.bytes += payload.data().length;
		upload.received++;
		if (upload.bytes > upload.totalBytes) {
			UPLOADS.remove(id);
			refuse(player, "bytes recibidos superan el total anunciado", "npcdirector.skin.import.corrupt", "[NPCDirector] Skin file rejected: it is not a valid PNG");
			return;
		}
		if (upload.received < upload.total) {
			return;
		}
		UPLOADS.remove(id);
		if (upload.bytes != upload.totalBytes) {
			refuse(player, "tamano final distinto del anunciado", "npcdirector.skin.import.corrupt", "[NPCDirector] Skin file rejected: it is not a valid PNG");
			return;
		}
		byte[] png = new byte[upload.bytes];
		int offset = 0;
		for (byte[] part : upload.parts) {
			System.arraycopy(part, 0, png, offset, part.length);
			offset += part.length;
		}
		SkinPng.Problem problem = SkinPng.check(png);
		if (problem == SkinPng.Problem.TOO_BIG) {
			refuse(player, "png demasiado grande", "npcdirector.skin.import.too_big", "[NPCDirector] Skin file rejected: it is larger than 16 KB");
			return;
		}
		if (problem == SkinPng.Problem.DIMENSIONS) {
			refuse(player, "dimensiones no validas", "npcdirector.skin.import.bad_size", "[NPCDirector] Skin file rejected: it must be 64x64 or 64x32 pixels");
			return;
		}
		if (problem != SkinPng.Problem.NONE) {
			refuse(player, "no es un png valido (" + problem + ")", "npcdirector.skin.import.corrupt", "[NPCDirector] Skin file rejected: it is not a valid PNG");
			return;
		}
		npc.setCustomSkin(png);
		message(player, "npcdirector.skin.import.ok", "[NPCDirector] Custom skin imported and applied (%s bytes)", png.length);
		DebugLog.action("skin importada por " + player.getName().getString() + " npc=" + npc.getId() + " bytes=" + png.length);
		OutputService.line("[NPC " + npc.getId() + "] skin importada " + png.length + " bytes por " + player.getName().getString());
	}

	public static void sendTo(ServerPlayer player, QuestNpcEntity npc) {
		String encoded = npc.getProfile().getSkinPng();
		if (encoded.isEmpty() || !NetworkingInit.canSend(player)) {
			return;
		}
		try {
			byte[] png = Base64.getDecoder().decode(encoded);
			NetworkingInit.sendToPlayer(player, new SkinSyncPayload(SkinPng.hash(png), png));
		} catch (IllegalArgumentException exception) {
			DebugLog.warn("skin importada ilegible en npc " + npc.getId());
		}
	}

	public static void sendToTrackers(QuestNpcEntity npc) {
		if (npc.level().isClientSide()) {
			return;
		}
		String encoded = npc.getProfile().getSkinPng();
		if (encoded.isEmpty()) {
			return;
		}
		try {
			byte[] png = Base64.getDecoder().decode(encoded);
			NetworkingInit.sendToTracking(npc, new SkinSyncPayload(SkinPng.hash(png), png));
		} catch (IllegalArgumentException exception) {
			DebugLog.warn("skin importada ilegible en npc " + npc.getId());
		}
	}
}
