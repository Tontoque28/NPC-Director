package com.tontoque28.npcdirector.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class QuestNpcRenderer extends HumanoidMobRenderer<QuestNpcEntity, PlayerModel<QuestNpcEntity>> {

	private static final double SIT_DROP = (12.0D - 2.9D) / 16.0D;

	private final PlayerModel<QuestNpcEntity> wideModel;
	private final PlayerModel<QuestNpcEntity> slimModel;

	public QuestNpcRenderer(EntityRenderDispatcher dispatcher) {
		super(dispatcher, new PlayerModel<QuestNpcEntity>(0.0F, false), 0.5F);
		this.wideModel = this.model;
		this.slimModel = new PlayerModel<QuestNpcEntity>(0.0F, true);
		this.addLayer(new HumanoidArmorLayer<QuestNpcEntity, PlayerModel<QuestNpcEntity>, HumanoidModel<QuestNpcEntity>>(this,
				new HumanoidModel<QuestNpcEntity>(0.5F), new HumanoidModel<QuestNpcEntity>(1.0F)));
	}

	@Override
	public void render(QuestNpcEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
		ClientSkins.Info info = ClientSkins.of(entity);
		this.model = ClientSkins.slim(entity, info) ? slimModel : wideModel;
		this.shadowRadius = 0.5F * entity.getSyncedScale();
		super.render(entity, yaw, partialTick, poseStack, buffer, light);
	}

	@Override
	protected void setupRotations(QuestNpcEntity entity, PoseStack poseStack, float bob, float bodyYaw, float partialTick) {
		super.setupRotations(entity, poseStack, bob, bodyYaw, partialTick);
		if (entity.isSyncedSitting()) {
			this.model.riding = true;
			poseStack.translate(0.0D, -SIT_DROP * entity.getSyncedScale(), 0.0D);
		}
	}

	@Override
	protected void scale(QuestNpcEntity entity, PoseStack poseStack, float partialTick) {
		float scale = entity.getSyncedScale();
		poseStack.scale(scale, scale, scale);
	}

	@Override
	public ResourceLocation getTextureLocation(QuestNpcEntity entity) {
		return ClientSkins.of(entity).texture();
	}
}
