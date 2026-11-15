package com.tontoque28.npcdirector.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

public class QuestNpcRenderer extends HumanoidMobRenderer<QuestNpcEntity, PlayerRenderState, PlayerModel> {

	private static final double SIT_DROP = (12.0D - 2.9D) / 16.0D;

	private final PlayerModel wideModel;
	private final PlayerModel slimModel;

	public QuestNpcRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
		this.wideModel = this.model;
		this.slimModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
		this.addLayer(new HumanoidArmorLayer<>(this,
				new HumanoidModel<PlayerRenderState>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
				new HumanoidModel<PlayerRenderState>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
				context.getEquipmentRenderer()));
	}

	@Override
	public PlayerRenderState createRenderState() {
		return new PlayerRenderState();
	}

	@Override
	public void extractRenderState(QuestNpcEntity entity, PlayerRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		ClientSkins.Info info = ClientSkins.of(entity);
		boolean slim = ClientSkins.slim(entity, info);
		state.skin = new PlayerSkin(info.texture(), null, null, null, slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, false);
		state.showHat = true;
		state.showJacket = true;
		state.showLeftPants = true;
		state.showRightPants = true;
		state.showLeftSleeve = true;
		state.showRightSleeve = true;
		state.isPassenger = entity.isSyncedSitting();
	}

	@Override
	public void render(PlayerRenderState state, PoseStack poseStack, MultiBufferSource buffer, int light) {
		this.model = state.skin.model() == PlayerSkin.Model.SLIM ? slimModel : wideModel;
		super.render(state, poseStack, buffer, light);
	}

	@Override
	protected void setupRotations(PlayerRenderState state, PoseStack poseStack, float bodyYaw, float scale) {
		super.setupRotations(state, poseStack, bodyYaw, scale);
		if (state.isPassenger) {
			poseStack.translate(0.0D, -SIT_DROP, 0.0D);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(PlayerRenderState state) {
		return state.skin.texture();
	}
}
