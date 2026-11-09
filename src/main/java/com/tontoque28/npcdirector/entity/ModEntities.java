package com.tontoque28.npcdirector.entity;

import com.tontoque28.npcdirector.NpcDirector;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

	public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, NpcDirector.MOD_ID);

	public static final RegistryObject<EntityType<QuestNpcEntity>> QUEST_NPC = ENTITY_TYPES.register("quest_npc",
			() -> EntityType.Builder.of(QuestNpcEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.95F)
					.build("quest_npc"));

	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(QUEST_NPC.get(), QuestNpcEntity.createAttributes().build());
	}
}
