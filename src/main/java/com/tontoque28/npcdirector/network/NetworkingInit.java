package com.tontoque28.npcdirector.network;

import java.util.List;
import java.util.stream.Collectors;

import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.client.ClientPacketHandler;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueNode;
import com.tontoque28.npcdirector.dialogue.DialogueOption;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkingInit {

	public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(NpcDirector.MOD_ID + ":main");

	public static void init() {
		CHANNEL.registerMessage(new OpenHandler(), OpenDialoguePayload.class, 0, Side.CLIENT);
		CHANNEL.registerMessage(new ChooseHandler(), ChooseDialoguePayload.class, 1, Side.SERVER);
		CHANNEL.registerMessage(new CloseHandler(), CloseDialoguePayload.class, 2, Side.CLIENT);
		CHANNEL.registerMessage(new CloseHandler(), CloseDialoguePayload.class, 2, Side.SERVER);
		CHANNEL.registerMessage(new OpenAdminHandler(), OpenAdminPayload.class, 3, Side.CLIENT);
		CHANNEL.registerMessage(new ApplyHandler(), ApplyAdminPayload.class, 4, Side.SERVER);
		CHANNEL.registerMessage(new DeleteHandler(), DeleteNpcPayload.class, 5, Side.SERVER);
		CHANNEL.registerMessage(new SummonHandler(), SummonNpcPayload.class, 6, Side.SERVER);
	}

	public static void sendToServer(IMessage message) {
		CHANNEL.sendToServer(message);
	}

	public static void sendToPlayer(EntityPlayerMP player, IMessage message) {
		CHANNEL.sendTo(message, player);
	}

	private static String cut(String text, int max) {
		if (text == null) {
			return "";
		}
		return text.length() > max ? text.substring(0, max) : text;
	}

	public static void sendNodeOrClose(EntityPlayerMP player, DialogueNode node) {
		if (node == null) {
			CHANNEL.sendTo(new CloseDialoguePayload(), player);
			return;
		}
		List<String> optionTexts = node.getOptions().stream()
				.limit(64)
				.map(option -> cut(option.getResponseText(), 512))
				.collect(Collectors.toList());
		CHANNEL.sendTo(new OpenDialoguePayload(cut(node.getText(), 8000), optionTexts), player);
	}

	public static class OpenHandler implements IMessageHandler<OpenDialoguePayload, IMessage> {
		@Override
		public IMessage onMessage(OpenDialoguePayload payload, MessageContext ctx) {
			ClientPacketHandler.open(payload.text(), payload.optionTexts());
			return null;
		}
	}

	public static class OpenAdminHandler implements IMessageHandler<OpenAdminPayload, IMessage> {
		@Override
		public IMessage onMessage(OpenAdminPayload payload, MessageContext ctx) {
			ClientPacketHandler.openAdmin(payload);
			return null;
		}
	}

	public static class ChooseHandler implements IMessageHandler<ChooseDialoguePayload, IMessage> {
		@Override
		public IMessage onMessage(ChooseDialoguePayload payload, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			int index = payload.optionIndex();
			player.getServerWorld().addScheduledTask(() -> {
				DialogueNode next = DialogueInteractionHandler.choose(player, index);
				sendNodeOrClose(player, next);
			});
			return null;
		}
	}

	public static class CloseHandler implements IMessageHandler<CloseDialoguePayload, IMessage> {
		@Override
		public IMessage onMessage(CloseDialoguePayload payload, MessageContext ctx) {
			if (ctx.side == Side.CLIENT) {
				ClientPacketHandler.close();
			} else {
				EntityPlayerMP player = ctx.getServerHandler().player;
				player.getServerWorld().addScheduledTask(() -> DialogueInteractionHandler.close(player));
			}
			return null;
		}
	}

	public static class ApplyHandler implements IMessageHandler<ApplyAdminPayload, IMessage> {
		@Override
		public IMessage onMessage(ApplyAdminPayload payload, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServerWorld().addScheduledTask(() -> AdminService.apply(player, payload));
			return null;
		}
	}

	public static class DeleteHandler implements IMessageHandler<DeleteNpcPayload, IMessage> {
		@Override
		public IMessage onMessage(DeleteNpcPayload payload, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			int id = payload.npcEntityId();
			player.getServerWorld().addScheduledTask(() -> AdminService.delete(player, id));
			return null;
		}
	}

	public static class SummonHandler implements IMessageHandler<SummonNpcPayload, IMessage> {
		@Override
		public IMessage onMessage(SummonNpcPayload payload, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServerWorld().addScheduledTask(() -> AdminService.summon(player));
			return null;
		}
	}
}
