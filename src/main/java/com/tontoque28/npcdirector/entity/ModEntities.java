package com.tontoque28.npcdirector.entity;

import com.tontoque28.npcdirector.NpcDirector;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

	private static final String DATA_FIXER_KEY = "villager";

	public static final EntityType<QuestNpcEntity> QUEST_NPC = Registry.register(
			Registry.ENTITY_TYPE,
			new ResourceLocation(NpcDirector.MOD_ID, "quest_npc"),
			EntityType.Builder.of(QuestNpcEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.95F)
					.build(DATA_FIXER_KEY)
	);

	public static void register() {
		FabricDefaultAttributeRegistry.register(QUEST_NPC, QuestNpcEntity.createAttributes());
	}
}
