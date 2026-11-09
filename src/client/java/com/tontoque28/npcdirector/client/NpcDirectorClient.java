package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.network.CloseDialoguePayload;
import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.network.OpenDialoguePayload;
import com.tontoque28.npcdirector.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class NpcDirectorClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.QUEST_NPC, QuestNpcRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(OpenDialoguePayload.ID, (payload, context) ->
				context.client().execute(() ->
						Minecraft.getInstance().setScreenAndShow(new DialogueScreen(payload.text(), payload.optionTexts()))));

		ClientPlayNetworking.registerGlobalReceiver(OpenAdminPayload.ID, (payload, context) ->
				context.client().execute(() -> Minecraft.getInstance().setScreenAndShow(new AdminScreen(payload))));

		ClientPlayNetworking.registerGlobalReceiver(CloseDialoguePayload.ID, (payload, context) ->
				context.client().execute(() -> {
					Minecraft minecraft = Minecraft.getInstance();
					if (minecraft.gui.screen() instanceof DialogueScreen) {
						minecraft.setScreenAndShow(null);
					}
				}));
	}
}
