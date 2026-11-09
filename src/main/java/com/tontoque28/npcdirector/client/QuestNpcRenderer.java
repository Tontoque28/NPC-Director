package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.QuestNpcEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class QuestNpcRenderer extends HumanoidMobRenderer<QuestNpcEntity, HumanoidModel<QuestNpcEntity>> {

	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/steve.png");

	public QuestNpcRenderer(EntityRenderDispatcher dispatcher) {
		super(dispatcher, new HumanoidModel<QuestNpcEntity>(0.0F), 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(QuestNpcEntity entity) {
		return TEXTURE;
	}
}
