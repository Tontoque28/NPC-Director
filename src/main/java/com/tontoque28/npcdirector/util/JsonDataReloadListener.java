package com.tontoque28.npcdirector.util;

import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.JsonElement;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

public class JsonDataReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {

	private final String folder;
	private final Consumer<Map<Identifier, JsonElement>> consumer;

	public JsonDataReloadListener(String folder, Consumer<Map<Identifier, JsonElement>> consumer) {
		super(ExtraCodecs.JSON, FileToIdConverter.json(folder));
		this.folder = folder;
		this.consumer = consumer;
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
		consumer.accept(prepared);
	}

	@Override
	public String getName() {
		return "npcdirector:" + folder;
	}
}
