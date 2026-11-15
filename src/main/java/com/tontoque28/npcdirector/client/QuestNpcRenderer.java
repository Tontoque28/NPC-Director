package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.entity.SeatHelper;

import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class QuestNpcRenderer extends RenderBiped<QuestNpcEntity> {

	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/steve.png");

	public QuestNpcRenderer(RenderManager manager) {
		super(manager, new ModelPlayer(0.0F, false), 0.5F);
	}

	@Override
	protected ResourceLocation getEntityTexture(QuestNpcEntity entity) {
		ResourceLocation custom = CustomSkins.get(entity.getSkinHashSynced());
		return custom != null ? custom : TEXTURE;
	}

	@Override
	public void doRender(QuestNpcEntity entity, double x, double y, double z, float entityYaw, float partialTicks) {
		double drop = entity.isSeatedSynced() ? SeatHelper.SIT_DROP : 0.0D;
		super.doRender(entity, x, y - drop, z, entityYaw, partialTicks);
	}

	@Override
	protected void renderModel(QuestNpcEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
		this.mainModel.isRiding = entity.isSeatedSynced();
		super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
	}
}
