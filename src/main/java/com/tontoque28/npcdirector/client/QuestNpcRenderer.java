package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.QuestNpcEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class QuestNpcRenderer extends HumanoidMobRenderer<QuestNpcEntity, HumanoidModel<QuestNpcEntity>> {

	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/player/wide/steve.png");

	public QuestNpcRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(QuestNpcEntity entity) {
		return TEXTURE;
	}
}
