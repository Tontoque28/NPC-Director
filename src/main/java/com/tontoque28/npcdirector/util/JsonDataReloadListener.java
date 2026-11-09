package com.tontoque28.npcdirector.util;

import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.JsonElement;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

public class JsonDataReloadListener extends SimpleJsonResourceReloadListener<JsonElement> implements IdentifiableResourceReloadListener {

	private final ResourceLocation fabricId;
	private final Consumer<Map<ResourceLocation, JsonElement>> consumer;

	public JsonDataReloadListener(ResourceLocation fabricId, String folder, Consumer<Map<ResourceLocation, JsonElement>> consumer) {
		super(ExtraCodecs.JSON, FileToIdConverter.json(folder));
		this.fabricId = fabricId;
		this.consumer = consumer;
	}

	@Override
	public ResourceLocation getFabricId() {
		return fabricId;
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
		consumer.accept(prepared);
	}
}
