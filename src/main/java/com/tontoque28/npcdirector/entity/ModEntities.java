package com.tontoque28.npcdirector.entity;

import com.tontoque28.npcdirector.NpcDirector;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public class ModEntities {

	public static void register() {
		EntityRegistry.registerModEntity(new ResourceLocation(NpcDirector.MOD_ID, "quest_npc"), QuestNpcEntity.class, "quest_npc", 0, NpcDirector.instance, 80, 3, true);
	}
}
