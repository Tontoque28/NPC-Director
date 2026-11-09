package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.ModEntities;

import net.minecraftforge.client.event.EntityRenderersEvent;

public class NpcDirectorClient {

	public static void register() {
		EntityRenderersEvent.RegisterRenderers.BUS.addListener(NpcDirectorClient::onRegisterRenderers);
	}

	private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ModEntities.QUEST_NPC.get(), QuestNpcRenderer::new);
	}
}
