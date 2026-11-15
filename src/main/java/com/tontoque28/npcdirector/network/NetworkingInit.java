package com.tontoque28.npcdirector.network;

import java.util.List;
import java.util.stream.Collectors;

import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueNode;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.output.OutputService;
import com.tontoque28.npcdirector.skin.CustomSkinService;

import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class NetworkingInit {

	public static void registerCommon() {
		PayloadTypeRegistry.playS2C().register(OpenDialoguePayload.ID, OpenDialoguePayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(CloseDialoguePayload.ID, CloseDialoguePayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ChooseDialoguePayload.ID, ChooseDialoguePayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(CloseDialoguePayload.ID, CloseDialoguePayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(OpenAdminPayload.ID, OpenAdminPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ApplyAdminPayload.ID, ApplyAdminPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(DeleteNpcPayload.ID, DeleteNpcPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(SummonNpcPayload.ID, SummonNpcPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ApplySkinPayload.ID, ApplySkinPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(SavePathPayload.ID, SavePathPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(SaveDialoguePayload.ID, SaveDialoguePayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(OutputSubscribePayload.ID, OutputSubscribePayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(OutputLinesPayload.ID, OutputLinesPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(AssignPathPayload.ID, AssignPathPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ImportSkinPayload.ID, ImportSkinPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SkinSyncPayload.ID, SkinSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(PanelMessagePayload.ID, PanelMessagePayload.STREAM_CODEC);
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
		ServerPlayNetworking.registerGlobalReceiver(ApplySkinPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> AdminService.applySkin(player, payload));
		});
		ServerPlayNetworking.registerGlobalReceiver(SavePathPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> AdminService.savePath(player, payload));
		});
		ServerPlayNetworking.registerGlobalReceiver(SaveDialoguePayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> AdminService.saveDialogue(player, payload));
		});
		ServerPlayNetworking.registerGlobalReceiver(OutputSubscribePayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> OutputService.subscribe(player, payload.on()));
		});
		ServerPlayNetworking.registerGlobalReceiver(AssignPathPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> AdminService.assignPath(player, payload));
		});
		ServerPlayNetworking.registerGlobalReceiver(ImportSkinPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> CustomSkinService.receive(player, payload));
		});
		EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
			if (entity instanceof QuestNpcEntity npc) {
				CustomSkinService.sendTo(player, npc);
			}
		});
	}

	public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}

	public static void sendPanel(ServerPlayer player, net.minecraft.network.chat.Component message) {
		if (ServerPlayNetworking.canSend(player, PanelMessagePayload.ID)) {
			ServerPlayNetworking.send(player, new PanelMessagePayload(message));
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
