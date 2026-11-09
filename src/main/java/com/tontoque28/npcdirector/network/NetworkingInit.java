package com.tontoque28.npcdirector.network;

import java.util.List;
import java.util.stream.Collectors;

import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueNode;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
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
		ServerPlayNetworking.registerGlobalReceiver(SUMMON_NPC, (server, player, handler, buf, responseSender) ->
				server.execute(() -> AdminService.summon(player)));
	}

	public static ResourceLocation idOf(Object message) {
		if (message instanceof OpenDialoguePayload) return OPEN_DIALOGUE;
		if (message instanceof ChooseDialoguePayload) return CHOOSE_DIALOGUE;
		if (message instanceof CloseDialoguePayload) return CLOSE_DIALOGUE;
		if (message instanceof OpenAdminPayload) return OPEN_ADMIN;
		if (message instanceof ApplyAdminPayload) return APPLY_ADMIN;
		if (message instanceof DeleteNpcPayload) return DELETE_NPC;
		if (message instanceof SummonNpcPayload) return SUMMON_NPC;
		throw new IllegalArgumentException("payload desconocido");
	}

	public static FriendlyByteBuf encode(Object message) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		if (message instanceof OpenDialoguePayload p) OpenDialoguePayload.encode(p, buf);
		else if (message instanceof ChooseDialoguePayload p) ChooseDialoguePayload.encode(p, buf);
		else if (message instanceof CloseDialoguePayload p) CloseDialoguePayload.encode(p, buf);
		else if (message instanceof OpenAdminPayload p) OpenAdminPayload.encode(p, buf);
		else if (message instanceof ApplyAdminPayload p) ApplyAdminPayload.encode(p, buf);
		else if (message instanceof DeleteNpcPayload p) DeleteNpcPayload.encode(p, buf);
		else if (message instanceof SummonNpcPayload p) SummonNpcPayload.encode(p, buf);
		return buf;
	}

	public static void sendToPlayer(ServerPlayer player, Object message) {
		ServerPlayNetworking.send(player, idOf(message), encode(message));
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
		sendToPlayer(player, new OpenDialoguePayload(cut(node.getText(), 8000), optionTexts));
	}

	private static String cut(String text, int max) {
		if (text == null) {
			return "";
		}
		return text.length() > max ? text.substring(0, max) : text;
	}
}
