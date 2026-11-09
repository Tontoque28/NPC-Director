package com.tontoque28.npcdirector.network;

import java.util.List;
import java.util.stream.Collectors;

import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.client.ClientPacketHandler;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueNode;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class NetworkingInit {

	public static final SimpleChannel CHANNEL = ChannelBuilder
			.named(Identifier.fromNamespaceAndPath(NpcDirector.MOD_ID, "main"))
			.networkProtocolVersion(1)
			.simpleChannel()
				.play()
					.clientbound()
						.addMain(OpenDialoguePayload.class, OpenDialoguePayload.STREAM_CODEC, NetworkingInit::onOpenDialogue)
					.serverbound()
						.addMain(ChooseDialoguePayload.class, ChooseDialoguePayload.STREAM_CODEC, NetworkingInit::onChooseDialogue)
					.bidirectional()
						.addMain(CloseDialoguePayload.class, CloseDialoguePayload.STREAM_CODEC, NetworkingInit::onCloseDialogue)
					.clientbound()
						.addMain(OpenAdminPayload.class, OpenAdminPayload.STREAM_CODEC, NetworkingInit::onOpenAdmin)
					.serverbound()
						.addMain(ApplyAdminPayload.class, ApplyAdminPayload.STREAM_CODEC, NetworkingInit::onApplyAdmin)
						.addMain(DeleteNpcPayload.class, DeleteNpcPayload.STREAM_CODEC, NetworkingInit::onDeleteNpc)
						.addMain(SummonNpcPayload.class, SummonNpcPayload.STREAM_CODEC, NetworkingInit::onSummonNpc)
			.build();

	public static void init() {
	}

	public static void sendToServer(Object message) {
		CHANNEL.send(message, PacketDistributor.SERVER.noArg());
	}

	public static void sendToPlayer(ServerPlayer player, Object message) {
		CHANNEL.send(message, PacketDistributor.PLAYER.with(player));
	}

	public static void sendNodeOrClose(ServerPlayer player, DialogueNode node) {
		if (node == null) {
			CHANNEL.send(new CloseDialoguePayload(), PacketDistributor.PLAYER.with(player));
			return;
		}
		List<String> optionTexts = node.getOptions().stream()
				.limit(64)
				.map(option -> cut(option.getResponseText(), 512))
				.collect(Collectors.toList());
		CHANNEL.send(new OpenDialoguePayload(cut(node.getText(), 8000), optionTexts), PacketDistributor.PLAYER.with(player));
	}

	private static String cut(String text, int max) {
		if (text == null) {
			return "";
		}
		return text.length() > max ? text.substring(0, max) : text;
	}

	private static void onOpenAdmin(OpenAdminPayload payload, CustomPayloadEvent.Context context) {
		ClientPacketHandler.openAdmin(payload);
	}

	private static void onApplyAdmin(ApplyAdminPayload payload, CustomPayloadEvent.Context context) {
		ServerPlayer sender = context.getSender();
		if (sender != null) {
			AdminService.apply(sender, payload);
		}
	}

	private static void onDeleteNpc(DeleteNpcPayload payload, CustomPayloadEvent.Context context) {
		ServerPlayer sender = context.getSender();
		if (sender != null) {
			AdminService.delete(sender, payload.npcEntityId());
		}
	}

	private static void onSummonNpc(SummonNpcPayload payload, CustomPayloadEvent.Context context) {
		ServerPlayer sender = context.getSender();
		if (sender != null) {
			AdminService.summon(sender);
		}
	}

	private static void onOpenDialogue(OpenDialoguePayload payload, CustomPayloadEvent.Context context) {
		ClientPacketHandler.open(payload.text(), payload.optionTexts());
	}

	private static void onChooseDialogue(ChooseDialoguePayload payload, CustomPayloadEvent.Context context) {
		ServerPlayer player = context.getSender();
		if (player == null) {
			return;
		}
		DialogueNode next = DialogueInteractionHandler.choose(player, payload.optionIndex());
		sendNodeOrClose(player, next);
	}

	private static void onCloseDialogue(CloseDialoguePayload payload, CustomPayloadEvent.Context context) {
		if (context.isClientSide()) {
			ClientPacketHandler.close();
			return;
		}
		ServerPlayer player = context.getSender();
		if (player != null) {
			DialogueInteractionHandler.close(player);
		}
	}
}
