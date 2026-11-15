package com.tontoque28.npcdirector.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;

public class QuestNpcRenderer extends HumanoidMobRenderer<QuestNpcEntity, AvatarRenderState, PlayerModel> {

	private static final double SIT_DROP = (12.0D - 2.9D) / 16.0D;

	private final PlayerModel wideModel;
	private final PlayerModel slimModel;

	public QuestNpcRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
		this.wideModel = this.model;
		this.slimModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
		this.addLayer(new ArmorByModel(this, ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel<AvatarRenderState>::new), context.getEquipmentRenderer(), false));
		this.addLayer(new ArmorByModel(this, ArmorModelSet.bake(ModelLayers.PLAYER_SLIM_ARMOR, context.getModelSet(), HumanoidModel<AvatarRenderState>::new), context.getEquipmentRenderer(), true));
	}

	@Override
	public AvatarRenderState createRenderState() {
		return new AvatarRenderState();
	}

	@Override
	public void extractRenderState(QuestNpcEntity entity, AvatarRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.skin = ClientSkins.resolve(entity);
		state.isPassenger = entity.isSyncedSitting();
		if (state.isPassenger) {
			state.y -= SIT_DROP * state.scale;
		}
		state.showHat = true;
		state.showJacket = true;
		state.showLeftPants = true;
		state.showRightPants = true;
		state.showLeftSleeve = true;
		state.showRightSleeve = true;
		state.shadowRadius = 0.5F * state.scale;
	}

	@Override
	public Identifier getTextureLocation(AvatarRenderState state) {
		return state.skin.body().texturePath();
	}

	private static class ArmorByModel extends HumanoidArmorLayer<AvatarRenderState, PlayerModel, HumanoidModel<AvatarRenderState>> {

		private final boolean slim;

		ArmorByModel(QuestNpcRenderer parent, ArmorModelSet<HumanoidModel<AvatarRenderState>> models, EquipmentLayerRenderer equipment, boolean slim) {
			super(parent, models, equipment);
			this.slim = slim;
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
			if ((state.skin.model() == PlayerModelType.SLIM) == slim) {
				super.submit(poseStack, collector, light, state, yRot, xRot);
			}
		}
	}
}
