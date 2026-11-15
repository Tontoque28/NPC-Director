package com.tontoque28.npcdirector.network;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.client.ClientPacketHandler;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueNode;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.output.OutputService;
import com.tontoque28.npcdirector.skin.CustomSkinService;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class NetworkingInit {

	private static final String PROTOCOL = "2";

	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(NpcDirector.MOD_ID, "main"),
			() -> PROTOCOL,
			PROTOCOL::equals,
			PROTOCOL::equals
	);

	private static int nextId = 0;

	private static <T> void toServer(Class<T> type, java.util.function.BiConsumer<T, net.minecraft.network.PacketBuffer> encoder,
			java.util.function.Function<net.minecraft.network.PacketBuffer, T> decoder, BiConsumer<ServerPlayerEntity, T> handler) {
		CHANNEL.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_SERVER)
				.encoder(encoder)
				.decoder(decoder)
				.consumer((T payload, Supplier<NetworkEvent.Context> context) -> {
					NetworkEvent.Context ctx = context.get();
					ServerPlayerEntity sender = ctx.getSender();
					if (sender != null) {
						ctx.enqueueWork(() -> handler.accept(sender, payload));
					}
					ctx.setPacketHandled(true);
				})
				.add();
	}

	private static <T> void toClient(Class<T> type, java.util.function.BiConsumer<T, net.minecraft.network.PacketBuffer> encoder,
			java.util.function.Function<net.minecraft.network.PacketBuffer, T> decoder, java.util.function.Consumer<T> handler) {
		CHANNEL.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(encoder)
				.decoder(decoder)
				.consumer((T payload, Supplier<NetworkEvent.Context> context) -> {
					NetworkEvent.Context ctx = context.get();
					ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handler.accept(payload)));
					ctx.setPacketHandled(true);
				})
				.add();
	}

	public static void init() {
		toClient(OpenDialoguePayload.class, OpenDialoguePayload::encode, OpenDialoguePayload::decode,
				payload -> ClientPacketHandler.open(payload.text(), payload.optionTexts(), payload.npcName()));
		toServer(ChooseDialoguePayload.class, ChooseDialoguePayload::encode, ChooseDialoguePayload::decode, (player, payload) -> {
			DialogueNode next = DialogueInteractionHandler.choose(player, payload.optionIndex());
			sendNodeOrClose(player, next);
		});
		CHANNEL.messageBuilder(CloseDialoguePayload.class, nextId++)
				.encoder(CloseDialoguePayload::encode)
				.decoder(CloseDialoguePayload::decode)
				.consumer((CloseDialoguePayload payload, Supplier<NetworkEvent.Context> context) -> {
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
				})
				.add();
		toClient(OpenAdminPayload.class, OpenAdminPayload::encode, OpenAdminPayload::decode, payload -> ClientPacketHandler.openAdmin(payload));
		toServer(ApplyAdminPayload.class, ApplyAdminPayload::encode, ApplyAdminPayload::decode, AdminService::apply);
		toServer(DeleteNpcPayload.class, DeleteNpcPayload::encode, DeleteNpcPayload::decode,
				(player, payload) -> AdminService.delete(player, payload.npcEntityId()));
		toServer(SummonNpcPayload.class, SummonNpcPayload::encode, SummonNpcPayload::decode,
				(player, payload) -> AdminService.summon(player));
		toServer(ApplySkinPayload.class, ApplySkinPayload::encode, ApplySkinPayload::decode, AdminService::applySkin);
		toServer(SavePathPayload.class, SavePathPayload::encode, SavePathPayload::decode, AdminService::savePath);
		toServer(SaveDialoguePayload.class, SaveDialoguePayload::encode, SaveDialoguePayload::decode, AdminService::saveDialogue);
		toServer(OutputSubscribePayload.class, OutputSubscribePayload::encode, OutputSubscribePayload::decode,
				(player, payload) -> OutputService.subscribe(player, payload.on()));
		toClient(OutputLinesPayload.class, OutputLinesPayload::encode, OutputLinesPayload::decode,
				payload -> ClientPacketHandler.output(payload.lines()));
		toServer(AssignPathPayload.class, AssignPathPayload::encode, AssignPathPayload::decode, AdminService::assignPath);
		toServer(ImportSkinPayload.class, ImportSkinPayload::encode, ImportSkinPayload::decode, CustomSkinService::receive);
		toClient(SkinSyncPayload.class, SkinSyncPayload::encode, SkinSyncPayload::decode,
				payload -> ClientPacketHandler.skinSync(payload.hash(), payload.png()));
		toClient(PanelMessagePayload.class, PanelMessagePayload::encode, PanelMessagePayload::decode,
				payload -> ClientPacketHandler.panelMessage(payload.message()));
	}

	public static void sendToServer(Object message) {
		CHANNEL.sendToServer(message);
	}

	public static void sendToPlayer(ServerPlayerEntity player, Object message) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
	}

	public static void sendToTrackers(QuestNpcEntity npc, Object message) {
		CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> npc), message);
	}

	public static void sendPanel(ServerPlayerEntity player, ITextComponent message) {
		sendToPlayer(player, new PanelMessagePayload(message));
	}

	public static void sendNodeOrClose(ServerPlayerEntity player, DialogueNode node) {
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
