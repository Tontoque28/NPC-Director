package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.ModEntities;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.network.OpenDialoguePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;

public class NpcDirectorClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.INSTANCE.register(ModEntities.QUEST_NPC, (dispatcher, context) -> new QuestNpcRenderer(dispatcher));

		ClientPlayNetworking.registerGlobalReceiver(NetworkingInit.OPEN_DIALOGUE, (client, handler, buf, responseSender) -> {
			OpenDialoguePayload payload = OpenDialoguePayload.decode(buf);
			client.execute(() -> client.setScreen(new DialogueScreen(payload.text(), payload.optionTexts())));
		});

		ClientPlayNetworking.registerGlobalReceiver(NetworkingInit.OPEN_ADMIN, (client, handler, buf, responseSender) -> {
			OpenAdminPayload payload = OpenAdminPayload.decode(buf);
			client.execute(() -> client.setScreen(new AdminScreen(payload)));
		});

		ClientPlayNetworking.registerGlobalReceiver(NetworkingInit.CLOSE_DIALOGUE, (client, handler, buf, responseSender) ->
				client.execute(() -> {
					Minecraft minecraft = Minecraft.getInstance();
					if (minecraft.screen instanceof DialogueScreen) {
						minecraft.setScreen(null);
					}
				}));
	}
}
