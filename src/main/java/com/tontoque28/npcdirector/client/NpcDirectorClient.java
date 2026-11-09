package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.ModEntities;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class NpcDirectorClient {

	public static void register(IEventBus modBus) {
		modBus.addListener(NpcDirectorClient::onRegisterRenderers);
	}

	private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ModEntities.QUEST_NPC.get(), QuestNpcRenderer::new);
	}
}
