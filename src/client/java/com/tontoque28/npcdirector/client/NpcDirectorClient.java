package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.ModEntities;
import com.tontoque28.npcdirector.network.CloseDialoguePayload;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.network.OpenDialoguePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;

public class NpcDirectorClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.QUEST_NPC, QuestNpcRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(OpenDialoguePayload.ID, (payload, context) ->
				context.client().execute(() ->
						Minecraft.getInstance().setScreen(new DialogueScreen(payload.text(), payload.optionTexts()))));

		ClientPlayNetworking.registerGlobalReceiver(OpenAdminPayload.ID, (payload, context) ->
				context.client().execute(() -> Minecraft.getInstance().setScreen(new AdminScreen(payload))));

		ClientPlayNetworking.registerGlobalReceiver(CloseDialoguePayload.ID, (payload, context) ->
				context.client().execute(() -> {
					Minecraft minecraft = Minecraft.getInstance();
					if (minecraft.screen instanceof DialogueScreen) {
						minecraft.setScreen(null);
					}
				}));
	}
}
