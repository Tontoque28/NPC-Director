package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.ModEntities;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.network.OpenDialoguePayload;
import com.tontoque28.npcdirector.network.OutputLinesPayload;
import com.tontoque28.npcdirector.network.PanelMessagePayload;
import com.tontoque28.npcdirector.network.SkinSyncPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;

public class NpcDirectorClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.INSTANCE.register(ModEntities.QUEST_NPC, (dispatcher, context) -> new QuestNpcRenderer(dispatcher));

		ClientPlayNetworking.registerGlobalReceiver(NetworkingInit.OPEN_DIALOGUE, (client, handler, buf, responseSender) -> {
			OpenDialoguePayload payload = OpenDialoguePayload.decode(buf);
			client.execute(() -> ClientPacketHandler.open(payload.text(), payload.optionTexts(), payload.npcName()));
		});

		ClientPlayNetworking.registerGlobalReceiver(NetworkingInit.OPEN_ADMIN, (client, handler, buf, responseSender) -> {
			OpenAdminPayload payload = OpenAdminPayload.decode(buf);
			client.execute(() -> ClientPacketHandler.openAdmin(payload));
		});

		ClientPlayNetworking.registerGlobalReceiver(NetworkingInit.OUTPUT_LINES, (client, handler, buf, responseSender) -> {
			OutputLinesPayload payload = OutputLinesPayload.decode(buf);
			client.execute(() -> ClientPacketHandler.output(payload.lines()));
		});

		ClientPlayNetworking.registerGlobalReceiver(NetworkingInit.CLOSE_DIALOGUE, (client, handler, buf, responseSender) ->
				client.execute(ClientPacketHandler::close));

		ClientPlayNetworking.registerGlobalReceiver(NetworkingInit.PANEL_MESSAGE, (client, handler, buf, responseSender) -> {
			PanelMessagePayload payload = PanelMessagePayload.decode(buf);
			client.execute(() -> ClientPacketHandler.panelMessage(payload.message()));
		});

		ClientPlayNetworking.registerGlobalReceiver(NetworkingInit.SKIN_SYNC, (client, handler, buf, responseSender) -> {
			SkinSyncPayload payload = SkinSyncPayload.decode(buf);
			client.execute(() -> ClientPacketHandler.skinSync(payload.hash(), payload.png()));
		});
	}
}
