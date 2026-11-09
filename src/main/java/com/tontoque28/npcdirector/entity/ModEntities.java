package com.tontoque28.npcdirector.entity;

import com.tontoque28.npcdirector.NpcDirector;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

	public static final EntityType<QuestNpcEntity> QUEST_NPC = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "quest_npc"),
			EntityType.Builder.of(QuestNpcEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.95F)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(NpcDirector.MOD_ID, "quest_npc")))
	);

	public static void register() {
		FabricDefaultAttributeRegistry.register(QUEST_NPC, QuestNpcEntity.createAttributes().build());
	}
}
