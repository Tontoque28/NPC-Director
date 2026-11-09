package com.tontoque28.npcdirector.network;

import java.util.List;
import java.util.stream.Collectors;

import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueNode;
import com.tontoque28.npcdirector.dialogue.DialogueOption;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class NetworkingInit {

	public static void registerCommon() {
		PayloadTypeRegistry.clientboundPlay().register(OpenDialoguePayload.ID, OpenDialoguePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CloseDialoguePayload.ID, CloseDialoguePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ChooseDialoguePayload.ID, ChooseDialoguePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CloseDialoguePayload.ID, CloseDialoguePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenAdminPayload.ID, OpenAdminPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ApplyAdminPayload.ID, ApplyAdminPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DeleteNpcPayload.ID, DeleteNpcPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SummonNpcPayload.ID, SummonNpcPayload.CODEC);
	}

	public static void registerServer() {
		ServerPlayNetworking.registerGlobalReceiver(ChooseDialoguePayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				DialogueNode next = DialogueInteractionHandler.choose(player, payload.optionIndex());
				sendNodeOrClose(player, next);
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(CloseDialoguePayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> DialogueInteractionHandler.close(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(ApplyAdminPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> AdminService.apply(player, payload));
		});
		ServerPlayNetworking.registerGlobalReceiver(DeleteNpcPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> AdminService.delete(player, payload.npcEntityId()));
		});
		ServerPlayNetworking.registerGlobalReceiver(SummonNpcPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> AdminService.summon(player));
		});
	}

	public static void sendToPlayer(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}

	public static void sendNodeOrClose(ServerPlayer player, DialogueNode node) {
		if (node == null) {
			ServerPlayNetworking.send(player, new CloseDialoguePayload());
			return;
		}
		List<String> optionTexts = node.getOptions().stream()
				.limit(64)
				.map(option -> cut(option.getResponseText(), 512))
				.collect(Collectors.toList());
		ServerPlayNetworking.send(player, new OpenDialoguePayload(cut(node.getText(), 8000), optionTexts));
	}

	private static String cut(String text, int max) {
		if (text == null) {
			return "";
		}
		return text.length() > max ? text.substring(0, max) : text;
	}
}
