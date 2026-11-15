package com.tontoque28.npcdirector.network;

import java.util.List;
import java.util.stream.Collectors;

import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.client.ClientPacketHandler;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueNode;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.output.OutputService;
import com.tontoque28.npcdirector.skin.CustomSkinService;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class NetworkingInit {

	public static final SimpleChannel CHANNEL = ChannelBuilder
			.named(Identifier.fromNamespaceAndPath(NpcDirector.MOD_ID, "main"))
			.networkProtocolVersion(3)
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
						.addMain(ApplySkinPayload.class, ApplySkinPayload.STREAM_CODEC, NetworkingInit::onApplySkin)
						.addMain(SavePathPayload.class, SavePathPayload.STREAM_CODEC, NetworkingInit::onSavePath)
						.addMain(SaveDialoguePayload.class, SaveDialoguePayload.STREAM_CODEC, NetworkingInit::onSaveDialogue)
						.addMain(OutputSubscribePayload.class, OutputSubscribePayload.STREAM_CODEC, NetworkingInit::onOutputSubscribe)
					.clientbound()
						.addMain(OutputLinesPayload.class, OutputLinesPayload.STREAM_CODEC, NetworkingInit::onOutputLines)
					.serverbound()
						.addMain(AssignPathPayload.class, AssignPathPayload.STREAM_CODEC, NetworkingInit::onAssignPath)
						.addMain(ImportSkinPayload.class, ImportSkinPayload.STREAM_CODEC, NetworkingInit::onImportSkin)
					.clientbound()
						.addMain(SkinSyncPayload.class, SkinSyncPayload.STREAM_CODEC, NetworkingInit::onSkinSync)
						.addMain(PanelMessagePayload.class, PanelMessagePayload.STREAM_CODEC, NetworkingInit::onPanelMessage)
			.build();

	public static void init() {
		PlayerEvent.StartTracking.BUS.addListener(NetworkingInit::onStartTracking);
	}

	private static void onStartTracking(PlayerEvent.StartTracking event) {
		if (event.getTarget() instanceof QuestNpcEntity npc && event.getEntity() instanceof ServerPlayer player) {
			CustomSkinService.sendTo(player, npc);
		}
	}

	public static void sendToTracking(Entity entity, Object message) {
		CHANNEL.send(message, PacketDistributor.TRACKING_ENTITY.with(entity));
	}

	public static void sendPanel(ServerPlayer player, Component message) {
		CHANNEL.send(new PanelMessagePayload(message), PacketDistributor.PLAYER.with(player));
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
		CHANNEL.send(new OpenDialoguePayload(cut(node.getText(), 8000), optionTexts, cut(DialogueInteractionHandler.npcName(player), 64)), PacketDistributor.PLAYER.with(player));
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

	private static void onPanelMessage(PanelMessagePayload payload, CustomPayloadEvent.Context context) {
		ClientPacketHandler.panelMessage(payload.message());
	}

	private static void onSkinSync(SkinSyncPayload payload, CustomPayloadEvent.Context context) {
		ClientPacketHandler.skinSync(payload.hash(), payload.png());
	}

	private static void onAssignPath(AssignPathPayload payload, CustomPayloadEvent.Context context) {
		ServerPlayer sender = context.getSender();
		if (sender != null) {
			AdminService.assignPath(sender, payload);
		}
	}

	private static void onImportSkin(ImportSkinPayload payload, CustomPayloadEvent.Context context) {
		ServerPlayer sender = context.getSender();
		if (sender != null) {
			CustomSkinService.receive(sender, payload);
		}
	}

	private static void onOutputLines(OutputLinesPayload payload, CustomPayloadEvent.Context context) {
		ClientPacketHandler.output(payload.lines());
	}

	private static void onApplyAdmin(ApplyAdminPayload payload, CustomPayloadEvent.Context context) {
		ServerPlayer sender = context.getSender();
		if (sender != null) {
			AdminService.apply(sender, payload);
		}
	}

	private static void onApplySkin(ApplySkinPayload payload, CustomPayloadEvent.Context context) {
		ServerPlayer sender = context.getSender();
		if (sender != null) {
			AdminService.applySkin(sender, payload);
		}
	}

	private static void onSavePath(SavePathPayload payload, CustomPayloadEvent.Context context) {
		ServerPlayer sender = context.getSender();
		if (sender != null) {
			AdminService.savePath(sender, payload);
		}
	}

	private static void onSaveDialogue(SaveDialoguePayload payload, CustomPayloadEvent.Context context) {
		ServerPlayer sender = context.getSender();
		if (sender != null) {
			AdminService.saveDialogue(sender, payload);
		}
	}

	private static void onOutputSubscribe(OutputSubscribePayload payload, CustomPayloadEvent.Context context) {
		ServerPlayer sender = context.getSender();
		if (sender != null) {
			OutputService.subscribe(sender, payload.on());
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
		ClientPacketHandler.open(payload.text(), payload.optionTexts(), payload.npcName());
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
