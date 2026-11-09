package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.QuestNpcEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

public class QuestNpcRenderer extends HumanoidMobRenderer<QuestNpcEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

	public QuestNpcRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return DefaultPlayerSkin.getDefaultTexture();
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}
}
