package com.tontoque28.npcdirector.network;

import java.util.List;
import java.util.stream.Collectors;

import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueNode;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.output.OutputService;
import com.tontoque28.npcdirector.skin.CustomSkinService;

import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class NetworkingInit {

	public static final ResourceLocation OPEN_DIALOGUE = new ResourceLocation(NpcDirector.MOD_ID, "open_dialogue");
	public static final ResourceLocation CLOSE_DIALOGUE = new ResourceLocation(NpcDirector.MOD_ID, "close_dialogue");
	public static final ResourceLocation CHOOSE_DIALOGUE = new ResourceLocation(NpcDirector.MOD_ID, "choose_dialogue");
	public static final ResourceLocation OPEN_ADMIN = new ResourceLocation(NpcDirector.MOD_ID, "open_admin");
	public static final ResourceLocation APPLY_ADMIN = new ResourceLocation(NpcDirector.MOD_ID, "apply_admin");
	public static final ResourceLocation DELETE_NPC = new ResourceLocation(NpcDirector.MOD_ID, "delete_npc");
	public static final ResourceLocation SUMMON_NPC = new ResourceLocation(NpcDirector.MOD_ID, "summon_npc");
	public static final ResourceLocation APPLY_SKIN = new ResourceLocation(NpcDirector.MOD_ID, "apply_skin");
	public static final ResourceLocation SAVE_PATH = new ResourceLocation(NpcDirector.MOD_ID, "save_path");
	public static final ResourceLocation SAVE_DIALOGUE = new ResourceLocation(NpcDirector.MOD_ID, "save_dialogue");
	public static final ResourceLocation OUTPUT_SUBSCRIBE = new ResourceLocation(NpcDirector.MOD_ID, "output_subscribe");
	public static final ResourceLocation OUTPUT_LINES = new ResourceLocation(NpcDirector.MOD_ID, "output_lines");

	public static final ResourceLocation ASSIGN_PATH = new ResourceLocation(NpcDirector.MOD_ID, "assign_path");
	public static final ResourceLocation IMPORT_SKIN = new ResourceLocation(NpcDirector.MOD_ID, "import_skin");
	public static final ResourceLocation SKIN_SYNC = new ResourceLocation(NpcDirector.MOD_ID, "skin_sync");
	public static final ResourceLocation PANEL_MESSAGE = new ResourceLocation(NpcDirector.MOD_ID, "panel_message");

	public static void registerCommon() {
	}

	public static void registerServer() {
		ServerPlayNetworking.registerGlobalReceiver(CHOOSE_DIALOGUE, (server, player, handler, buf, responseSender) -> {
			ChooseDialoguePayload payload = ChooseDialoguePayload.decode(buf);
			server.execute(() -> {
				DialogueNode next = DialogueInteractionHandler.choose(player, payload.optionIndex());
				sendNodeOrClose(player, next);
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(CLOSE_DIALOGUE, (server, player, handler, buf, responseSender) ->
				server.execute(() -> DialogueInteractionHandler.close(player)));
		ServerPlayNetworking.registerGlobalReceiver(APPLY_ADMIN, (server, player, handler, buf, responseSender) -> {
			ApplyAdminPayload payload = ApplyAdminPayload.decode(buf);
			server.execute(() -> AdminService.apply(player, payload));
		});
		ServerPlayNetworking.registerGlobalReceiver(DELETE_NPC, (server, player, handler, buf, responseSender) -> {
			DeleteNpcPayload payload = DeleteNpcPayload.decode(buf);
			server.execute(() -> AdminService.delete(player, payload.npcEntityId()));
		});
		ServerPlayNetworking.registerGlobalReceiver(APPLY_SKIN, (server, player, handler, buf, responseSender) -> {
			ApplySkinPayload payload = ApplySkinPayload.decode(buf);
			server.execute(() -> AdminService.applySkin(player, payload));
		});
		ServerPlayNetworking.registerGlobalReceiver(SAVE_PATH, (server, player, handler, buf, responseSender) -> {
			SavePathPayload payload = SavePathPayload.decode(buf);
			server.execute(() -> AdminService.savePath(player, payload));
		});
		ServerPlayNetworking.registerGlobalReceiver(SAVE_DIALOGUE, (server, player, handler, buf, responseSender) -> {
			SaveDialoguePayload payload = SaveDialoguePayload.decode(buf);
			server.execute(() -> AdminService.saveDialogue(player, payload));
		});
		ServerPlayNetworking.registerGlobalReceiver(OUTPUT_SUBSCRIBE, (server, player, handler, buf, responseSender) -> {
			OutputSubscribePayload payload = OutputSubscribePayload.decode(buf);
			server.execute(() -> OutputService.subscribe(player, payload.on()));
		});
		ServerPlayNetworking.registerGlobalReceiver(SUMMON_NPC, (server, player, handler, buf, responseSender) ->
				server.execute(() -> AdminService.summon(player)));
		ServerPlayNetworking.registerGlobalReceiver(ASSIGN_PATH, (server, player, handler, buf, responseSender) -> {
			AssignPathPayload payload = AssignPathPayload.decode(buf);
			server.execute(() -> AdminService.assignPath(player, payload));
		});
		ServerPlayNetworking.registerGlobalReceiver(IMPORT_SKIN, (server, player, handler, buf, responseSender) -> {
			ImportSkinPayload payload = ImportSkinPayload.decode(buf);
			server.execute(() -> CustomSkinService.receive(player, payload));
		});
		EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
			if (entity instanceof QuestNpcEntity) {
				CustomSkinService.sendTo(player, (QuestNpcEntity) entity);
			}
		});
	}

	public static ResourceLocation idOf(Object message) {
		if (message instanceof OpenDialoguePayload) return OPEN_DIALOGUE;
		if (message instanceof ChooseDialoguePayload) return CHOOSE_DIALOGUE;
		if (message instanceof CloseDialoguePayload) return CLOSE_DIALOGUE;
		if (message instanceof OpenAdminPayload) return OPEN_ADMIN;
		if (message instanceof ApplyAdminPayload) return APPLY_ADMIN;
		if (message instanceof DeleteNpcPayload) return DELETE_NPC;
		if (message instanceof SummonNpcPayload) return SUMMON_NPC;
		if (message instanceof ApplySkinPayload) return APPLY_SKIN;
		if (message instanceof SavePathPayload) return SAVE_PATH;
		if (message instanceof SaveDialoguePayload) return SAVE_DIALOGUE;
		if (message instanceof OutputSubscribePayload) return OUTPUT_SUBSCRIBE;
		if (message instanceof OutputLinesPayload) return OUTPUT_LINES;
		if (message instanceof AssignPathPayload) return ASSIGN_PATH;
		if (message instanceof ImportSkinPayload) return IMPORT_SKIN;
		if (message instanceof SkinSyncPayload) return SKIN_SYNC;
		if (message instanceof PanelMessagePayload) return PANEL_MESSAGE;
		throw new IllegalArgumentException("payload desconocido");
	}

	public static FriendlyByteBuf encode(Object message) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		if (message instanceof OpenDialoguePayload) OpenDialoguePayload.encode((OpenDialoguePayload) message, buf);
		else if (message instanceof ChooseDialoguePayload) ChooseDialoguePayload.encode((ChooseDialoguePayload) message, buf);
		else if (message instanceof CloseDialoguePayload) CloseDialoguePayload.encode((CloseDialoguePayload) message, buf);
		else if (message instanceof OpenAdminPayload) OpenAdminPayload.encode((OpenAdminPayload) message, buf);
		else if (message instanceof ApplyAdminPayload) ApplyAdminPayload.encode((ApplyAdminPayload) message, buf);
		else if (message instanceof DeleteNpcPayload) DeleteNpcPayload.encode((DeleteNpcPayload) message, buf);
		else if (message instanceof SummonNpcPayload) SummonNpcPayload.encode((SummonNpcPayload) message, buf);
		else if (message instanceof ApplySkinPayload) ApplySkinPayload.encode((ApplySkinPayload) message, buf);
		else if (message instanceof SavePathPayload) SavePathPayload.encode((SavePathPayload) message, buf);
		else if (message instanceof SaveDialoguePayload) SaveDialoguePayload.encode((SaveDialoguePayload) message, buf);
		else if (message instanceof OutputSubscribePayload) OutputSubscribePayload.encode((OutputSubscribePayload) message, buf);
		else if (message instanceof OutputLinesPayload) OutputLinesPayload.encode((OutputLinesPayload) message, buf);
		else if (message instanceof AssignPathPayload) AssignPathPayload.encode((AssignPathPayload) message, buf);
		else if (message instanceof ImportSkinPayload) ImportSkinPayload.encode((ImportSkinPayload) message, buf);
		else if (message instanceof SkinSyncPayload) SkinSyncPayload.encode((SkinSyncPayload) message, buf);
		else if (message instanceof PanelMessagePayload) PanelMessagePayload.encode((PanelMessagePayload) message, buf);
		return buf;
	}

	public static void sendToPlayer(ServerPlayer player, Object message) {
		ServerPlayNetworking.send(player, idOf(message), encode(message));
	}

	public static void sendPanel(ServerPlayer player, Component message) {
		if (ServerPlayNetworking.canSend(player, PANEL_MESSAGE)) {
			sendToPlayer(player, new PanelMessagePayload(message));
		}
	}

	public static void sendNodeOrClose(ServerPlayer player, DialogueNode node) {
		if (node == null) {
			sendToPlayer(player, new CloseDialoguePayload());
			return;
		}
		List<String> optionTexts = node.getOptions().stream()
				.limit(64)
				.map(option -> cut(option.getResponseText(), 512))
				.collect(Collectors.toList());
		sendToPlayer(player, new OpenDialoguePayload(cut(node.getText(), 8000), optionTexts, cut(DialogueInteractionHandler.npcName(player), 64)));
	}

	private static String cut(String text, int max) {
		if (text == null) {
			return "";
		}
		return text.length() > max ? text.substring(0, max) : text;
	}
}
