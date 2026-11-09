package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.QuestNpcEntity;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.util.ResourceLocation;

public class QuestNpcRenderer extends BipedRenderer<QuestNpcEntity, BipedModel<QuestNpcEntity>> {

	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/steve.png");

	public QuestNpcRenderer(EntityRendererManager dispatcher) {
		super(dispatcher, new BipedModel<QuestNpcEntity>(0.0F), 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(QuestNpcEntity entity) {
		return TEXTURE;
	}
}
