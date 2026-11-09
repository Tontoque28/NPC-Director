package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.QuestNpcEntity;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class QuestNpcRenderer extends RenderBiped<QuestNpcEntity> {

	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/steve.png");

	public QuestNpcRenderer(RenderManager manager) {
		super(manager, new ModelBiped(0.0F), 0.5F);
	}

	@Override
	protected ResourceLocation getEntityTexture(QuestNpcEntity entity) {
		return TEXTURE;
	}
}
