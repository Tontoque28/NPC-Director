package com.tontoque28.npcdirector.entity;

import com.tontoque28.npcdirector.NpcDirector;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EntityClassification;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntities {

	public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, NpcDirector.MOD_ID);

	public static final RegistryObject<EntityType<QuestNpcEntity>> QUEST_NPC = ENTITY_TYPES.register("quest_npc",
			() -> EntityType.Builder.of(QuestNpcEntity::new, EntityClassification.MISC)
					.sized(0.6F, 1.95F)
					.build("quest_npc"));

	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(QUEST_NPC.get(), QuestNpcEntity.createAttributes().build());
	}
}
