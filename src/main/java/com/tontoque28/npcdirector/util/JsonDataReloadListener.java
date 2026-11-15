package com.tontoque28.npcdirector.util;

import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import net.minecraft.util.ResourceLocation;
import net.minecraft.resources.IResourceManager;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.profiler.IProfiler;

public class JsonDataReloadListener extends JsonReloadListener {

	private static final Gson GSON = new Gson();

	private final Consumer<Map<ResourceLocation, JsonElement>> consumer;

	public JsonDataReloadListener(String folder, Consumer<Map<ResourceLocation, JsonElement>> consumer) {
		super(GSON, folder);
		this.consumer = consumer;
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> prepared, IResourceManager resourceManager, IProfiler profiler) {
		consumer.accept(prepared);
	}
}
