package com.tontoque28.npcdirector.util;

import java.util.UUID;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;

public class Texts {

	public static MutableComponent literal(String text) {
		return new TextComponent(text);
	}

	public static MutableComponent translatable(String key, Object... args) {
		return new TranslatableComponent(key, args);
	}

	public static void tell(Entity entity, Component message) {
		entity.sendMessage(message, entity.getUUID());
	}

	public static void tell(net.minecraft.commands.CommandSource source, Component message) {
		source.sendMessage(message, Util.NIL_UUID);
	}
}
