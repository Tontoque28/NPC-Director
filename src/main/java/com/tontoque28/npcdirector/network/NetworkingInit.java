package com.tontoque28.npcdirector.network;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.client.ClientPacketHandler;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueNode;
import com.tontoque28.npcdirector.dialogue.DialogueOption;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkingInit {

	private static final String PROTOCOL = "1";

	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(NpcDirector.MOD_ID, "main"),
			() -> PROTOCOL,
			PROTOCOL::equals,
			PROTOCOL::equals
	);

	public static void init() {
		CHANNEL.messageBuilder(OpenDialoguePayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(OpenDialoguePayload::encode)
				.decoder(OpenDialoguePayload::decode)
				.consumerMainThread(NetworkingInit::onOpenDialogue)
				.add();
		CHANNEL.messageBuilder(ChooseDialoguePayload.class, 1, NetworkDirection.PLAY_TO_SERVER)
				.encoder(ChooseDialoguePayload::encode)
				.decoder(ChooseDialoguePayload::decode)
				.consumerMainThread(NetworkingInit::onChooseDialogue)
				.add();
		CHANNEL.messageBuilder(CloseDialoguePayload.class, 2)
				.encoder(CloseDialoguePayload::encode)
				.decoder(CloseDialoguePayload::decode)
				.consumerMainThread(NetworkingInit::onCloseDialogue)
				.add();
		CHANNEL.messageBuilder(OpenAdminPayload.class, 3, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(OpenAdminPayload::encode)
				.decoder(OpenAdminPayload::decode)
				.consumerMainThread(NetworkingInit::onOpenAdmin)
				.add();
		CHANNEL.messageBuilder(ApplyAdminPayload.class, 4, NetworkDirection.PLAY_TO_SERVER)
				.encoder(ApplyAdminPayload::encode)
				.decoder(ApplyAdminPayload::decode)
				.consumerMainThread((payload, context) -> {
					ServerPlayer sender = context.get().getSender();
					if (sender != null) {
						AdminService.apply(sender, payload);
					}
				})
				.add();
		CHANNEL.messageBuilder(DeleteNpcPayload.class, 5, NetworkDirection.PLAY_TO_SERVER)
				.encoder(DeleteNpcPayload::encode)
				.decoder(DeleteNpcPayload::decode)
				.consumerMainThread((payload, context) -> {
					ServerPlayer sender = context.get().getSender();
					if (sender != null) {
						AdminService.delete(sender, payload.npcEntityId());
					}
				})
				.add();
		CHANNEL.messageBuilder(SummonNpcPayload.class, 6, NetworkDirection.PLAY_TO_SERVER)
				.encoder(SummonNpcPayload::encode)
				.decoder(SummonNpcPayload::decode)
				.consumerMainThread((payload, context) -> {
					ServerPlayer sender = context.get().getSender();
					if (sender != null) {
						AdminService.summon(sender);
					}
				})
				.add();
	}

	public static void sendToPlayer(ServerPlayer player, Object message) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
	}

	private static void onOpenAdmin(OpenAdminPayload payload, Supplier<NetworkEvent.Context> context) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openAdmin(payload));
	}

	public static void sendToServer(Object message) {
		CHANNEL.sendToServer(message);
	}

	public static void sendNodeOrClose(ServerPlayer player, DialogueNode node) {
		if (node == null) {
			CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CloseDialoguePayload());
			return;
		}
		List<String> optionTexts = node.getOptions().stream()
				.limit(64)
				.map(option -> cut(option.getResponseText(), 512))
				.collect(Collectors.toList());
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenDialoguePayload(cut(node.getText(), 8000), optionTexts));
	}

	private static String cut(String text, int max) {
		if (text == null) {
			return "";
		}
		return text.length() > max ? text.substring(0, max) : text;
	}

	private static void onOpenDialogue(OpenDialoguePayload payload, Supplier<NetworkEvent.Context> context) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.open(payload.text(), payload.optionTexts()));
	}

	private static void onChooseDialogue(ChooseDialoguePayload payload, Supplier<NetworkEvent.Context> context) {
		ServerPlayer player = context.get().getSender();
		if (player == null) {
			return;
		}
		DialogueNode next = DialogueInteractionHandler.choose(player, payload.optionIndex());
		sendNodeOrClose(player, next);
	}

	private static void onCloseDialogue(CloseDialoguePayload payload, Supplier<NetworkEvent.Context> context) {
		if (context.get().getDirection().getReceptionSide().isClient()) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientPacketHandler::close);
			return;
		}
		ServerPlayer player = context.get().getSender();
		if (player != null) {
			DialogueInteractionHandler.close(player);
		}
	}
}
