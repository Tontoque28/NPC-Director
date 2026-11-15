package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.ModEntities;
import com.tontoque28.npcdirector.network.CloseDialoguePayload;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.network.OpenDialoguePayload;
import com.tontoque28.npcdirector.network.OutputLinesPayload;
import com.tontoque28.npcdirector.network.PanelMessagePayload;
import com.tontoque28.npcdirector.network.SkinSyncPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class NpcDirectorClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.QUEST_NPC, QuestNpcRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(OpenDialoguePayload.ID, (payload, context) ->
				context.client().execute(() -> ClientPacketHandler.open(payload.text(), payload.optionTexts(), payload.npcName())));

		ClientPlayNetworking.registerGlobalReceiver(OpenAdminPayload.ID, (payload, context) ->
				context.client().execute(() -> ClientPacketHandler.openAdmin(payload)));

		ClientPlayNetworking.registerGlobalReceiver(CloseDialoguePayload.ID, (payload, context) ->
				context.client().execute(ClientPacketHandler::close));

		ClientPlayNetworking.registerGlobalReceiver(OutputLinesPayload.ID, (payload, context) ->
				context.client().execute(() -> ClientPacketHandler.output(payload.lines())));

		ClientPlayNetworking.registerGlobalReceiver(PanelMessagePayload.ID, (payload, context) ->
				context.client().execute(() -> ClientPacketHandler.panelMessage(payload.message())));

		ClientPlayNetworking.registerGlobalReceiver(SkinSyncPayload.ID, (payload, context) ->
				context.client().execute(() -> ClientPacketHandler.skinSync(payload.hash(), payload.png())));
	}
}
