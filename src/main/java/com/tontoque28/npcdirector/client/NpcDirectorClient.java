package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.QuestNpcEntity;

import net.minecraftforge.fml.client.registry.RenderingRegistry;

public class NpcDirectorClient {

	public static void register() {
		RenderingRegistry.registerEntityRenderingHandler(QuestNpcEntity.class, QuestNpcRenderer::new);
	}
}
