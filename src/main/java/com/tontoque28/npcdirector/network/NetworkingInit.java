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

import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

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
				.consumer(NetworkingInit::onOpenDialogue)
				.add();
		CHANNEL.messageBuilder(ChooseDialoguePayload.class, 1, NetworkDirection.PLAY_TO_SERVER)
				.encoder(ChooseDialoguePayload::encode)
				.decoder(ChooseDialoguePayload::decode)
				.consumer(NetworkingInit::onChooseDialogue)
				.add();
		CHANNEL.messageBuilder(CloseDialoguePayload.class, 2)
				.encoder(CloseDialoguePayload::encode)
				.decoder(CloseDialoguePayload::decode)
				.consumer(NetworkingInit::onCloseDialogue)
				.add();
		CHANNEL.messageBuilder(OpenAdminPayload.class, 3, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(OpenAdminPayload::encode)
				.decoder(OpenAdminPayload::decode)
				.consumer(NetworkingInit::onOpenAdmin)
				.add();
		CHANNEL.messageBuilder(ApplyAdminPayload.class, 4, NetworkDirection.PLAY_TO_SERVER)
				.encoder(ApplyAdminPayload::encode)
				.decoder(ApplyAdminPayload::decode)
				.consumer((payload, context) -> {
					NetworkEvent.Context ctx = context.get();
					ServerPlayerEntity sender = ctx.getSender();
					if (sender != null) {
						ctx.enqueueWork(() -> AdminService.apply(sender, payload));
					}
					ctx.setPacketHandled(true);
				})
				.add();
		CHANNEL.messageBuilder(DeleteNpcPayload.class, 5, NetworkDirection.PLAY_TO_SERVER)
				.encoder(DeleteNpcPayload::encode)
				.decoder(DeleteNpcPayload::decode)
				.consumer((payload, context) -> {
					NetworkEvent.Context ctx = context.get();
					ServerPlayerEntity sender = ctx.getSender();
					if (sender != null) {
						ctx.enqueueWork(() -> AdminService.delete(sender, payload.npcEntityId()));
					}
					ctx.setPacketHandled(true);
				})
				.add();
		CHANNEL.messageBuilder(SummonNpcPayload.class, 6, NetworkDirection.PLAY_TO_SERVER)
				.encoder(SummonNpcPayload::encode)
				.decoder(SummonNpcPayload::decode)
				.consumer((payload, context) -> {
					NetworkEvent.Context ctx = context.get();
					ServerPlayerEntity sender = ctx.getSender();
					if (sender != null) {
						ctx.enqueueWork(() -> AdminService.summon(sender));
					}
					ctx.setPacketHandled(true);
				})
				.add();
	}

	public static void sendToServer(Object message) {
		CHANNEL.sendToServer(message);
	}

	public static void sendToPlayer(ServerPlayerEntity player, Object message) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
	}

	public static void sendNodeOrClose(ServerPlayerEntity player, DialogueNode node) {
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
		NetworkEvent.Context ctx = context.get();
		ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.open(payload.text(), payload.optionTexts())));
		ctx.setPacketHandled(true);
	}

	private static void onOpenAdmin(OpenAdminPayload payload, Supplier<NetworkEvent.Context> context) {
		NetworkEvent.Context ctx = context.get();
		ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openAdmin(payload)));
		ctx.setPacketHandled(true);
	}

	private static void onChooseDialogue(ChooseDialoguePayload payload, Supplier<NetworkEvent.Context> context) {
		NetworkEvent.Context ctx = context.get();
		ServerPlayerEntity player = ctx.getSender();
		if (player != null) {
			ctx.enqueueWork(() -> {
				DialogueNode next = DialogueInteractionHandler.choose(player, payload.optionIndex());
				sendNodeOrClose(player, next);
			});
		}
		ctx.setPacketHandled(true);
	}

	private static void onCloseDialogue(CloseDialoguePayload payload, Supplier<NetworkEvent.Context> context) {
		NetworkEvent.Context ctx = context.get();
		if (ctx.getDirection().getReceptionSide().isClient()) {
			ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientPacketHandler::close));
		} else {
			ServerPlayerEntity player = ctx.getSender();
			if (player != null) {
				ctx.enqueueWork(() -> DialogueInteractionHandler.close(player));
			}
		}
		ctx.setPacketHandled(true);
	}
}
