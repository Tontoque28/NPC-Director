package com.tontoque28.npcdirector.skin;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.data.NpcProfile;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.network.ImportSkinPayload;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.network.SkinSyncPayload;
import com.tontoque28.npcdirector.util.DebugLog;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

public final class CustomSkinService {

	public static final int MAX_CHUNKS = 8;
	public static final long COOLDOWN_MS = 2000L;
	public static final long UPLOAD_TIMEOUT_MS = 30000L;

	private static final Map<UUID, Upload> UPLOADS = new HashMap<>();
	private static final Map<UUID, Long> LAST_START = new HashMap<>();

	private static final class Upload {
		int npcId;
		int sendId;
		int total;
		int totalBytes;
		int next;
		long started;
		boolean broken;
		byte[][] chunks;
		int received;
	}

	private CustomSkinService() {
	}

	private static void drop(UUID player) {
		UPLOADS.remove(player);
	}

	public static void forget(UUID player) {
		UPLOADS.remove(player);
		LAST_START.remove(player);
	}

	public static void receive(EntityPlayerMP player, ImportSkinPayload payload) {
		UUID id = player.getUniqueID();
		long now = System.currentTimeMillis();
		if (!AdminService.canEdit(player)) {
			if (payload.index() == 0) {
				DebugLog.denied("importar skin denegado por permisos, modo jugador o no creativo: " + player.getName());
			}
			drop(id);
			return;
		}
		Upload upload = UPLOADS.get(id);
		if (upload != null && now - upload.started > UPLOAD_TIMEOUT_MS) {
			drop(id);
			upload = null;
		}
		if (payload.index() == 0) {
			Long last = LAST_START.get(id);
			if (last != null && now - last < COOLDOWN_MS) {
				fail(player, "npcdirector.skin.import_rate");
				drop(id);
				return;
			}
			LAST_START.put(id, now);
			if (AdminService.validate(player, payload.npcEntityId(), "importar skin") == null) {
				drop(id);
				return;
			}
			int total = payload.total();
			int totalBytes = payload.totalBytes();
			boolean clear = total == 1 && totalBytes == 0 && payload.chunk().length == 0;
			if (clear) {
				drop(id);
				QuestNpcEntity npc = AdminService.validate(player, payload.npcEntityId(), "quitar skin");
				if (npc != null) {
					npc.getProfile().setSkinPng("");
					npc.setProfile(npc.getProfile());
					broadcast(npc);
					AdminService.tell(player, "npcdirector.skin.cleared");
				}
				return;
			}
			if (total < 1 || total > MAX_CHUNKS || totalBytes < 1 || totalBytes > SkinPng.MAX_BYTES || payload.chunk().length < 1) {
				fail(player, "npcdirector.skin.import_bad");
				drop(id);
				return;
			}
			upload = new Upload();
			upload.npcId = payload.npcEntityId();
			upload.sendId = payload.sendId();
			upload.total = total;
			upload.totalBytes = totalBytes;
			upload.started = now;
			upload.chunks = new byte[total][];
			UPLOADS.put(id, upload);
		} else if (upload == null || upload.broken) {
			return;
		}
		if (payload.sendId() != upload.sendId || payload.npcEntityId() != upload.npcId || payload.total() != upload.total
				|| payload.totalBytes() != upload.totalBytes || payload.index() != upload.next || payload.chunk().length < 1
				|| payload.chunk().length > ImportSkinPayload.CHUNK_MAX) {
			upload.broken = true;
			fail(player, "npcdirector.skin.import_bad");
			drop(id);
			return;
		}
		upload.chunks[upload.next++] = payload.chunk();
		upload.received += payload.chunk().length;
		if (upload.received > upload.totalBytes) {
			fail(player, "npcdirector.skin.import_bad");
			drop(id);
			return;
		}
		if (upload.next < upload.total) {
			return;
		}
		drop(id);
		if (upload.received != upload.totalBytes) {
			fail(player, "npcdirector.skin.import_bad");
			return;
		}
		byte[] data = new byte[upload.received];
		int pos = 0;
		for (byte[] chunk : upload.chunks) {
			System.arraycopy(chunk, 0, data, pos, chunk.length);
			pos += chunk.length;
		}
		SkinPng.Problem problem = SkinPng.check(data);
		if (problem != SkinPng.Problem.NONE) {
			fail(player, problem == SkinPng.Problem.DIMENSIONS ? "npcdirector.skin.import_size" : "npcdirector.skin.import_bad");
			return;
		}
		QuestNpcEntity npc = AdminService.validate(player, upload.npcId, "importar skin");
		if (npc == null) {
			return;
		}
		NpcProfile profile = npc.getProfile();
		profile.setSkinPng(Base64.getEncoder().encodeToString(data));
		if (profile.getSkinPng().isEmpty()) {
			fail(player, "npcdirector.skin.import_bad");
			return;
		}
		npc.setProfile(profile);
		broadcast(npc);
		AdminService.tell(player, "npcdirector.skin.imported");
		DebugLog.action("skin importada por " + player.getName() + " hash=" + profile.getSkinPngHash());
	}

	private static void fail(EntityPlayerMP player, String key) {
		DebugLog.denied("importar skin rechazado (" + key + ") de " + player.getName());
		AdminService.tell(player, key);
	}

	public static void broadcast(QuestNpcEntity npc) {
		if (!(npc.world instanceof WorldServer)) {
			return;
		}
		SkinSyncPayload payload = payloadOf(npc);
		for (net.minecraft.entity.player.EntityPlayer other : ((WorldServer) npc.world).getEntityTracker().getTrackingPlayers(npc)) {
			if (other instanceof EntityPlayerMP) {
				NetworkingInit.sendToPlayer((EntityPlayerMP) other, payload);
			}
		}
	}

	public static void syncTo(EntityPlayerMP player, QuestNpcEntity npc) {
		if (npc.getProfile().getSkinPngHash().isEmpty()) {
			return;
		}
		NetworkingInit.sendToPlayer(player, payloadOf(npc));
	}

	private static SkinSyncPayload payloadOf(QuestNpcEntity npc) {
		NpcProfile profile = npc.getProfile();
		byte[] png = new byte[0];
		if (!profile.getSkinPng().isEmpty()) {
			try {
				png = Base64.getDecoder().decode(profile.getSkinPng());
			} catch (IllegalArgumentException exception) {
				png = new byte[0];
			}
		}
		return new SkinSyncPayload(npc.getEntityId(), png.length == 0 ? "" : profile.getSkinPngHash(), png);
	}
}
