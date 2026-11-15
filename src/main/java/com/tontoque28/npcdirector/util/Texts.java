package com.tontoque28.npcdirector.util;

import java.util.UUID;

import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.entity.Entity;

public class Texts {

	public static IFormattableTextComponent literal(String text) {
		return new StringTextComponent(text);
	}

	public static IFormattableTextComponent translatable(String key, Object... args) {
		return new TranslationTextComponent(key, args);
	}

	public static void tell(Entity entity, ITextComponent message) {
		entity.sendMessage(message, entity.getUUID());
	}

	public static void tell(net.minecraft.command.ICommandSource source, ITextComponent message) {
		source.sendMessage(message, Util.NIL_UUID);
	}
}
