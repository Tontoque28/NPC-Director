package com.tontoque28.npcdirector.util;

import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class JsonDataReloadListener extends SimpleJsonResourceReloadListener {

	private static final Gson GSON = new Gson();

	private final String folder;
	private final Consumer<Map<ResourceLocation, JsonElement>> consumer;

	public JsonDataReloadListener(String folder, Consumer<Map<ResourceLocation, JsonElement>> consumer) {
		super(GSON, folder);
		this.folder = folder;
		this.consumer = consumer;
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
		consumer.accept(prepared);
	}

	@Override
	public String getName() {
		return "npcdirector:" + folder;
	}
}
