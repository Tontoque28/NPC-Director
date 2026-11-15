package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.ModEntities;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class NpcDirectorClient {

	public static void register(IEventBus modBus) {
		modBus.addListener(NpcDirectorClient::onClientSetup);
	}

	private static void onClientSetup(FMLClientSetupEvent event) {
		RenderingRegistry.registerEntityRenderingHandler(ModEntities.QUEST_NPC.get(), QuestNpcRenderer::new);
	}
}
